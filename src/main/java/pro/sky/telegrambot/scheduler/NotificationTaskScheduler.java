package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationTaskScheduler {
    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final NotificationTaskRepository repository;
    private final TelegramBot telegramBot;

    @Autowired
    public NotificationTaskScheduler(NotificationTaskRepository repository, TelegramBot telegramBot) {
        this.repository = repository;
        this.telegramBot = telegramBot;
    }
    //Метод каждую минуту проверяет есть ли подходящие уведомление для данного времени
    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotifications() {
        LocalDateTime nowDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<NotificationTask> notificationTasks = repository.findAllByDateTime(nowDateTime);

        notificationTasks.forEach(this::sendNotification);
    }

    //Метод отправляет уведомление в чат и если оно успешно добавилось то удаляет его из БД
    private void sendNotification(NotificationTask task) {
        SendResponse response = telegramBot.execute(new SendMessage(task.getChatId(), task.getMessage()));

        if (response.isOk()) {
            repository.delete(task);
        }
    }
}