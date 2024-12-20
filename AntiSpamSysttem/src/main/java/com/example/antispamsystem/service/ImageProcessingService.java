package com.example.antispamsystem.service;


import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.modality.cv.Image;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import com.example.antispamsystem.utils.CubicSpline;

public class ImageProcessingService {
    //private Model model; // Загружаем модель DeepLab;
    // Минимальный порог сходства для определения нормальности траектории (75%)
    private static final double MINIMUM_SIMILARITY_THRESHOLD = 75.0;
    private static final double LINEARITY_ANGLE_THRESHOLD = 2.0; // Порог для допустимого отклонения угла
    private static final double STRAIGHTNESS_THRESHOLD = 0.95;   // Порог для допустимого отклонения от прямой
    private static final double TRAJECTORY_WEIGHT = 1;
    private static final double TYPING_SPEED_WEIGHT = 0.8;
    private static final double KEY_PRESS_WEIGHT = 0.15;
    private static final double TYPO_COUNT_WEIGHT = 0.05;
    // Заглушки для средних значений (пока нет интеграции с БД)
    private static final double AVERAGE_TYPING_SPEED = 0.8;  // Средняя скорость между нажатиями в секундах
    private static final int AVERAGE_TYPO_COUNT = 5;        // Среднее количество опечаток


    // Метод для обработки траектории и сравнения с эталонными
    public int processAndCompareTrajectory(String imagePath, String baseDirectory, double typingSpeed, int typoCount, List<Integer> typingSpeeds, List<Integer> keyPressDurations){
        try (NDManager manager = NDManager.newBaseManager()) {
            Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));
            List<Point2D> currentTrajectory = extractTrajectoryFeatures(img);
            double similarity = 0;
            if (currentTrajectory.isEmpty()) {
                System.out.println("Извлеченная траектория пуста, возможно ошибка в логике извлечения.");
                return 0;
            }

            if (isTrajectoryCompletelyLinear(currentTrajectory)) {
                System.out.println("Траектория линейная. Подозрение на бота.");
                return 0;
            }
            // Определяем количество точек для текущей траектории
            int targetPointCount = currentTrajectory.size();
            if(targetPointCount <= 250) {
                System.out.println("Извлеченная траектория слишком мала. Подозрение на робота.");
                return 0;
            }
            // Рассчитываем среднюю траекторию с учетом количества точек в currentTrajectory
            List<Point2D> averageTrajectory = calculateAverageTrajectory(baseDirectory, targetPointCount);

            if (averageTrajectory == null) {
                System.out.println("Нет эталонных траекторий. Сохраняем текущее изображение как эталонное.");
                similarity = 100;
            }
            else {
                similarity = calculateTrajectorySimilarity(currentTrajectory, averageTrajectory);
            }
            System.out.println("Сходство траекторий: " + similarity);
            double typingSpeedScore = calculateTypingSpeedScore(typingSpeed);
            System.out.println("typingSpeedScore: " + typingSpeedScore);
            int ts = 0;
            if(!typingSpeeds.isEmpty()) ts = typingScriptMonotone(typingSpeeds);
            int kpd = 0;
            if(!keyPressDurations.isEmpty()) kpd = keyPressDurationMonotone(keyPressDurations);
            double typoScore = calculateTypoScore(typoCount);
            double finalKeyScore = (TYPING_SPEED_WEIGHT * ts + KEY_PRESS_WEIGHT * kpd + TYPO_COUNT_WEIGHT * typoScore)* 100;
            boolean trajectoryIsNormal = similarity >= MINIMUM_SIMILARITY_THRESHOLD;
            boolean keyScoreIsNormal = finalKeyScore >= MINIMUM_SIMILARITY_THRESHOLD;
            if(trajectoryIsNormal && keyScoreIsNormal) {
                return 11;
            }
            else {
                if(!trajectoryIsNormal && keyScoreIsNormal) {
                    return 1;
                }
                else {
                    if(trajectoryIsNormal && !keyScoreIsNormal) {
                        return 10;
                    }
                    else {
                        return 0;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Общая ошибка в процессе обработки и сравнения траекторий.");
            e.printStackTrace();
            return 0;
        }
    }
    int typingScriptMonotone(List<Integer> typingSpeeds) {
        double min = Collections.min(typingSpeeds);
        double max = Collections.max((typingSpeeds));
        if(min == max) {
            return 0;
        }
        else {
            return 1;
        }
    }
    int keyPressDurationMonotone(List<Integer> kpd) {
        double min = Collections.min(kpd);
        double max = Collections.max((kpd));
        if(min == max) {
            return 0;
        }
        else {
            return 1;
        }
    }

    // Метод для проверки, состоит ли траектория из прямых линий
    public boolean isTrajectoryCompletelyLinear(List<Point2D> trajectory) {
        int size = trajectory.size();

        // Если меньше 3 точек, траектория считается линейной по умолчанию
        if (size < 3) return true;

        // Используем IntStream для параллельной обработки
        return IntStream.range(1, size - 1).parallel().allMatch(i -> {
            Point2D p1 = trajectory.get(i - 1);
            Point2D p2 = trajectory.get(i);
            Point2D p3 = trajectory.get(i + 1);

            // Проверка, является ли сегмент p1-p2 прямой линией
            if (!isStraightLine(p1, p2)) {
                return false;
            }

            // Проверка угла между сегментами p1-p2 и p2-p3
            double angle = calculateAngleBetweenSegments(p1, p2, p3);
            return isSuspiciousAngle(angle); // Проверка подозрительности угла
        });
    }

    // Метод для проверки, является ли сегмент прямой линией
    private boolean isStraightLine(Point2D p1, Point2D p2) {
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();
        double angle = Math.atan2(deltaY, deltaX) * (180 / Math.PI);

        return (Math.abs(angle) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 90) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 180) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 270) <= LINEARITY_ANGLE_THRESHOLD);
    }

    // Метод для проверки подозрительности угла
    private boolean isSuspiciousAngle(double angle) {
        return (Math.abs(angle) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 90) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 180) <= LINEARITY_ANGLE_THRESHOLD ||
                Math.abs(angle - 270) <= LINEARITY_ANGLE_THRESHOLD);
    }

    // Метод для вычисления угла между двумя сегментами
    private double calculateAngleBetweenSegments(Point2D p1, Point2D p2, Point2D p3) {
        double vector1X = p2.getX() - p1.getX();
        double vector1Y = p2.getY() - p1.getY();
        double vector2X = p3.getX() - p2.getX();
        double vector2Y = p3.getY() - p2.getY();

        double dotProduct = vector1X * vector2X + vector1Y * vector2Y;
        double length1 = Math.sqrt(vector1X * vector1X + vector1Y * vector1Y);
        double length2 = Math.sqrt(vector2X * vector2X + vector2Y * vector2Y);

        double cosTheta = dotProduct / (length1 * length2);
        return Math.acos(cosTheta) * (180 / Math.PI);
    }
    private double calculateTypingSpeedScore(double typingSpeed) {
        if (typingSpeed <= AVERAGE_TYPING_SPEED) {
            return 1.0;  // Высшая оценка, если скорость выше или равна средней
        } else {
            double relativeDeviation = Math.abs(typingSpeed - AVERAGE_TYPING_SPEED) / AVERAGE_TYPING_SPEED;
            return Math.max(0, Math.min(1, 1 - relativeDeviation));  // Низшая оценка, если скорость ниже средней
        }
    }

    private double calculateTypoScore(int typoCount) {
        if (typoCount >= AVERAGE_TYPO_COUNT) {
            return 1.0;  // Высшая оценка, если опечаток больше или равно средней величине
        } else {
            double relativeDeviation = Math.abs(typoCount - AVERAGE_TYPO_COUNT) / (double) AVERAGE_TYPO_COUNT;
            return Math.max(0, Math.min(1, 1 - relativeDeviation));  // Низшая оценка, если количество опечаток ниже среднего
        }
    }

    private List<Point2D> extractTrajectoryFeatures(Image image) {
        List<Point2D> trajectoryPoints = new ArrayList<>();
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = image.toNDArray(manager).toType(DataType.FLOAT32, false);
            if (array.getShape().dimension() < 3) {
                System.out.println("Изображение не имеет 3 каналов RGB.");
                return trajectoryPoints;
            }

            int height = (int) array.getShape().get(0);
            int width = (int) array.getShape().get(1);

            NDArray redChannel = array.get(new NDIndex(":, :, 0"));
            NDArray greenChannel = array.get(new NDIndex(":, :, 1"));
            NDArray blueChannel = array.get(new NDIndex(":, :, 2"));

            // Используем параллельные потоки для обработки строк изображения
            trajectoryPoints = Stream.iterate(0, y -> y + 1)
                    .limit(height)
                    .parallel()
                    .flatMap(y -> Stream.iterate(0, x -> x + 1)
                            .limit(width)
                            .map(x -> {
                                float blueValue = blueChannel.getFloat(y, x);
                                float redValue = redChannel.getFloat(y, x);
                                float greenValue = greenChannel.getFloat(y, x);

                                // Условие для определения точки на основе значений каналов
                                if (blueValue > 0.5f && redValue < 0.5f && greenValue < 0.5f) {
                                    return (Point2D) new Point2D.Double(x, y); // Приведение типа к Point2D
                                } else {
                                    return null;
                                }
                            })
                    )
                    .filter(point -> point != null)
                    .toList(); // Сохраняем в List<Point2D>

            if (trajectoryPoints.isEmpty()) {
                System.out.println("Извлеченные точки траектории пусты.");
            }

        } catch (Exception e) {
            System.out.println("Ошибка при извлечении особенностей траектории.");
            e.printStackTrace();
        }
        return trajectoryPoints;
    }
    //TODO: Написать свою модель НС с нуля, а не просто использовать их механизмы (на основе метода  extractTrajectoryFeatures можно написать и обучить модель)

    // Метод для извлечения точек из выхода модели
    private List<Point2D> extractPointsFromOutput(NDArray output, int width, int height) {
        List<Point2D> points = new ArrayList<>();
        float[] flatOutput = output.toFloatArray(); // Преобразуем результат в массив

        for (int i = 0; i < flatOutput.length; i += 2) { // Каждая точка состоит из x и y
            float x = flatOutput[i];
            float y = flatOutput[i + 1];

            // Проверяем границы
            if (x >= 0 && x < width && y >= 0 && y < height) {
                points.add(new Point2D.Float(x, y));
            }
        }
        return points;
    }


    // Метод для расчета средней траектории
    private List<Point2D> calculateAverageTrajectory(String baseDirectory, int targetPointCount) {
        List<List<Point2D>> normalizedTrajectories = new ArrayList<>();
        try (Stream<Path> files = Files.list(Paths.get(baseDirectory))) {
            for (Path filePath : files.filter(file -> file.toString().endsWith(".png")).toArray(Path[]::new)) {
                // Извлекаем траекторию из изображения
                Image img = ImageFactory.getInstance().fromFile(filePath);
                List<Point2D> trajectory = extractTrajectoryFeatures(img);

                // Нормализуем траекторию до targetPointCount точек (зависит от currentTrajectory)
                List<Point2D> normalizedTrajectory = normalizeTrajectory(trajectory, targetPointCount);
                normalizedTrajectories.add(normalizedTrajectory);
            }

            // Если не найдено ни одной траектории, возвращаем null
            if (normalizedTrajectories.isEmpty()) return null;

            // Рассчитываем среднюю траекторию для нормализованных данных
            return calculateAverageFromNormalized(normalizedTrajectories, targetPointCount);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Метод для нормализации траектории с использованием кубических сплайнов и параллельной обработки
    public List<Point2D> normalizeTrajectory(List<Point2D> trajectory, int targetCount) {
        int size = trajectory.size();
        if (size == targetCount) {
            return new ArrayList<>(trajectory);
        }
        // Если исходных точек больше, чем целевых, выбираем подмножество точек
        if (size > targetCount) {
            // Пропорционально уменьшаем количество точек
            return IntStream.range(0, targetCount)
                    .mapToObj(i -> {
                        // Пропорционально выбираем индексы из исходной траектории
                        int index = (int) ((double) i / (targetCount - 1) * (size - 1));
                        return trajectory.get(index);
                    })
                    .collect(Collectors.toList());
        }
        // Вычисление накопленной длины пути
        double[] t = new double[size];
        t[0] = 0.0; // Нулевая длина для первой точки
        for (int i = 1; i < size; i++) {
            Point2D p1 = trajectory.get(i - 1);
            Point2D p2 = trajectory.get(i);
            t[i] = t[i - 1] + p1.distance(p2); // Суммируем расстояния между точками
        }

        // Разделение координат x и y
        double[] xPoints = trajectory.stream().mapToDouble(Point2D::getX).toArray();
        double[] yPoints = trajectory.stream().mapToDouble(Point2D::getY).toArray();

        // Создание кубических сплайнов для x(t) и y(t)
        CubicSpline splineX = new CubicSpline(t, xPoints);
        CubicSpline splineY = new CubicSpline(t, yPoints);

        // Генерация нормализованных точек
        double totalLength = t[size - 1];
        List<Point2D> result = IntStream.range(0, targetCount)
                .mapToObj(i -> {
                    double targetT = (double) i / (targetCount - 1) * totalLength; // Новое значение параметра t
                    double newX = splineX.interpolate(targetT);
                    double newY = splineY.interpolate(targetT);
                    return new Point2D.Double(newX, newY);
                })
                .collect(Collectors.toList());
        return result;
    }


    // Метод для вычисления средней траектории из нормализованных данных
    public List<Point2D> calculateAverageFromNormalized(List<List<Point2D>> trajectories, int pointCount) {
        int trajectoryCount = trajectories.size();

        // Инициализируем список точек суммами (0,0)
        List<Point2D> sumTrajectory = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            sumTrajectory.add(new Point2D.Double(0, 0));
        }

        // Параллельное суммирование всех координат по каждой точке
        trajectories.parallelStream().forEach(trajectory -> {
            for (int i = 0; i < trajectory.size(); i++) {
                Point2D current = trajectory.get(i);
                Point2D sum = sumTrajectory.get(i);
                // Обновляем сумму координат каждой точки
                sumTrajectory.set(i, new Point2D.Double(sum.getX() + current.getX(), sum.getY() + current.getY()));
            }
        });

        // Параллельное деление на количество траекторий для получения среднего значения
        return IntStream.range(0, pointCount)
                .parallel()  // Включаем параллелизм
                .mapToObj(i -> {
                    Point2D sum = sumTrajectory.get(i);
                    // Возвращаем объект Point2D с усреднёнными координатами
                    return new Point2D.Double(sum.getX() / trajectoryCount, sum.getY() / trajectoryCount);
                })
                .collect(Collectors.toList());  // Собираем в список Point2D
    }

    // Обновленный метод для расчета итогового сходства на основе обновленной формулы
    private double calculateTrajectorySimilarity(List<Point2D> current, List<Point2D> average) {
        double manhattanDistance = calculateManhattanDistance(current, average);
        double cosineSimilarity = calculateCosineSimilarity(current, average);

        // Нормализация расстояния и косинусного сходства
        double normalizedManhattan = 1.0 / (1.0 + Math.log(1 + manhattanDistance));
        double normalizedCosine = (cosineSimilarity + 1.0) / 2.0;  // Приводим косинусное сходство к диапазону [0, 1]
        double similarity = 0;
        if(manhattanDistance > 500 || cosineSimilarity < 0.85) {
            similarity = 0;
        }
        else {
            // Обновленная формула расчета итогового сходства
            similarity = 100.0 * (normalizedManhattan * 0.25 + normalizedCosine * 0.75);
        }
        System.out.println("Манхэттенское расстояние: " + manhattanDistance);
        System.out.println("Косинусное сходство: " + cosineSimilarity);
        System.out.println("Итоговое сходство: " + similarity);
        return similarity;
    }

    // Метод для расчета Манхэттенского расстояния
    public double calculateManhattanDistance(List<Point2D> current, List<Point2D> average) {
        DoubleAdder sum = new DoubleAdder();  // потокобезопасная сумма

        int count = Math.min(current.size(), average.size());

        // Используем IntStream для параллельной обработки
        IntStream.range(0, count).parallel().forEach(i -> {
            Point2D p1 = current.get(i);
            Point2D p2 = average.get(i);
            // Вычисляем Манхэттенское расстояние для пары точек
            sum.add(Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY()));
        });

        // Возвращаем среднее Манхэттенское расстояние
        return sum.sum() / count;
    }


    // Метод для расчета косинусного сходства
    public double calculateCosineSimilarity(List<Point2D> current, List<Point2D> average) {
        DoubleAdder dotProduct = new DoubleAdder();  // потокобезопасное сложение
        DoubleAdder normA = new DoubleAdder();       // для нормы A
        DoubleAdder normB = new DoubleAdder();       // для нормы B

        int count = Math.min(current.size(), average.size());

        // Используем IntStream для параллельной обработки
        IntStream.range(0, count).parallel().forEach(i -> {
            double x1 = current.get(i).getX();
            double y1 = current.get(i).getY();
            double x2 = average.get(i).getX();
            double y2 = average.get(i).getY();

            // Вычисляем и добавляем результаты для каждой пары точек
            dotProduct.add(x1 * x2 + y1 * y2);
            normA.add(x1 * x1 + y1 * y1);
            normB.add(x2 * x2 + y2 * y2);
        });

        // Возвращаем результат косинусного сходства
        return dotProduct.sum() / (Math.sqrt(normA.sum()) * Math.sqrt(normB.sum()));
    }

    // Метод для удаления изображения, если оно признано подозрительным
    public void deleteImage(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Изображение успешно удалено: " + imagePath);
            } else {
                System.out.println("Не удалось удалить изображение: " + imagePath);
            }
        } else {
            System.out.println("Файл не найден: " + imagePath);
        }
    }
    // Метод для перемещения изображения в эталонные траектории
    public void moveImageToDirectory(String sourcePath, String targetDirectory) {
        File sourceFile = new File(sourcePath);
        File targetDir = new File(targetDirectory);

        if (!targetDir.exists()) {
            targetDir.mkdirs(); // Создаем директорию, если она не существует
        }

        // Перемещаем файл
        File targetFile = new File(targetDir, sourceFile.getName());
        boolean success = sourceFile.renameTo(targetFile);
        if (success) {
            System.out.println("Файл успешно перемещен в: " + targetDirectory);
        } else {
            System.out.println("Не удалось переместить файл: " + sourcePath);
        }
    }
}