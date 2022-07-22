package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final String START_COMMAND = "/start";
    private static final String HELLO_TEXT = "Hello!";
    private static final DateTimeFormatter DATA_TIME= DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String REGEX_BOT_MESSAGE = "([\\d\\.\\:\\s]{16})(\\s)([\\W+]+)";
    private static final LocalDateTime CURRENT_DATE = LocalDateTime.now();

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    @Autowired
    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            // Текст сообщения из чата
            String text = update.message().text();

            //ID чата
            Long chatId = update.message().chat().id();

            //Получаем текст сообщения из чата и отправляем ответ
            Message message = update.message();
            if (message.text().equals(START_COMMAND)) {
                logger.info(START_COMMAND + "received");
                sendMessage(message.chat().id(), HELLO_TEXT);
                return;
            }

            Matcher matcher = Pattern.compile(REGEX_BOT_MESSAGE).matcher(text);
            if (!matcher.matches()) {
                sendMessage(chatId, "Введено некорректное сообщение!");
                return;
            }

            // Дата и время уведомления
            String notificationDataTimeStr = matcher.group(1);

            //Текст уведомления
            String notificationText = matcher.group(3);

            //Парсинг строки в LocalDateTime
            LocalDateTime notificationDataTime = LocalDateTime
                    .parse(notificationDataTimeStr, DATA_TIME);
            if (notificationDataTime.isBefore(CURRENT_DATE)) {
                sendMessage(chatId, "Дата указана неверно!");
                return;
            }

            //Создаем уведомление
            NotificationTask notificationTask = new NotificationTask(chatId, notificationText, notificationDataTime);

            //Добавляем уведомление в БД
            notificationTask = notificationTaskRepository.save(notificationTask);

            //Отправляем уведомление
            sendMessage(chatId, "Напоминание! " + notificationTask.getMessage()
                    + ". Я отправлю Вам уведомление " + notificationTask.getDateTime().format(DATA_TIME));
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        telegramBot.execute(sendMessage);
    }


}
