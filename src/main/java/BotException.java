import java.util.logging.Logger;

public class BotException extends Exception {

    private Logger logger = Main.initLogger("BotExceptions", "BotExceptions");

    BotException(String message, String errorStackTrace) {
        logger.severe(message);
        logger.severe(errorStackTrace);
    }


}
