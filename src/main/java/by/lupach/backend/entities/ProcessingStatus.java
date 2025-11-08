package by.lupach.backend.entities;

public enum ProcessingStatus {
    PENDING,       // Файл загружен, ожидает обработки
    PROCESSING,    // В процессе анализа
    COMPLETED,     // Анализ успешно завершен
    FAILED,        // Ошибка при анализе
    CANCELLED      // Анализ отменен пользователем
}
