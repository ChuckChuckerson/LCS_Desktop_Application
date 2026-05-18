package com.lawyercompany;

import com.lawyercompany.controller.*;
import com.lawyercompany.entity.Case;
import com.lawyercompany.entity.Lawyer;
import com.lawyercompany.entity.User;
import com.lawyercompany.up.api.UpHttpServer;
import com.lawyercompany.service.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.Taskbar;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.imageio.ImageIO;

public class MainApp extends Application {

    private Stage primaryStage;
    private User currentUser;
    private Image appIcon;
    private final UserService userService = new UserService();
    private View currentView = View.LOGIN;
    private UpHttpServer upHttpServer;
    
    private enum View {
        LOGIN,
        REGISTER,
        CLIENT,
        LAWYER,
        ADMIN
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.appIcon = loadAppIcon();
        applyAppIcon(primaryStage);
        applyTaskbarIcon();
        startUpHttpServerIfNeeded();
        //это стартовая точка: показываем логин и дальше уже переключаем сцены внутри одного primaryStage
        showLoginWindow();
        primaryStage.show();
    }

    private void startUpHttpServerIfNeeded() {
        try {
            this.upHttpServer = new UpHttpServer();
            this.upHttpServer.start(UpHttpServer.DEFAULT_PORT);
            System.out.println("[UP] HttpServer started on " + UpHttpServer.localBaseUrl());
        } catch (Exception e) {
            System.out.println("[UP] Failed to start HttpServer: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            if (upHttpServer != null) upHttpServer.stop();
        } catch (Exception ignored) {
        }
    }

    private Image loadAppIcon() {
        try (InputStream is = getClass().getResourceAsStream("/icons/app.png")) {
            if (is == null) return null;
            return new Image(is);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyAppIcon(Stage stage) {
        if (stage == null || appIcon == null) return;
        stage.getIcons().clear();
        stage.getIcons().add(appIcon);
    }

    private void applySceneAutoSize(Stage stage, Parent root, String title, boolean applyIcon) {
        applySceneAutoSize(stage, root, title, applyIcon, null);
    }

    private void applySceneAutoSize(Stage stage, Parent root, String title, boolean applyIcon, View view) {
        if (applyIcon) {
            applyAppIcon(stage);
        }
        boolean isPrimary = stage == primaryStage;
        boolean preserveSize = false;
        double oldW = -1;
        double oldH = -1;
        if (isPrimary && stage.isShowing() && view != null) {
            //это чтобы primaryStage не "прыгал" при refresh (но при переходе login<->dashboard размер сбрасываем)
            preserveSize = (this.currentView == view);
            oldW = stage.getWidth();
            oldH = stage.getHeight();
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(true);

        if (preserveSize) {
            stage.setWidth(oldW);
            stage.setHeight(oldH);
        } else {
            //это подгоняет окно под контент текущей сцены, иначе логин мог остаться огромным после дашборда
            stage.sizeToScene();
            stage.centerOnScreen();
        }

        clampStageToScreen(stage);

        if (isPrimary && view != null) {
            this.currentView = view;
        }
    }

    private void clampStageToScreen(Stage stage) {
        if (stage == null) return;
        //берём экран, где сейчас окно (важно для 2-х мониторов и масштабирования 125%/150%)
        Screen screen = Screen.getPrimary();
        try {
            var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(),
                    Math.max(1, stage.getWidth()), Math.max(1, stage.getHeight()));
            if (!screens.isEmpty()) {
                screen = screens.get(0);
            }
        } catch (Exception ignored) {
        }

        var vb = screen.getVisualBounds();

        //на Windows нужен запас под рамку/заголовок: иначе крестик может "уехать" за пределы
        //при масштабировании (125%/150%) размеры рамки растут — учитываем outputScale.
        double scaleX = 1.0;
        double scaleY = 1.0;
        try {
            scaleX = Math.max(1.0, screen.getOutputScaleX());
            scaleY = Math.max(1.0, screen.getOutputScaleY());
        } catch (Throwable ignored) {
        }

        double marginLeftRight = 16 * scaleX;
        double marginTop = 10 * scaleY;
        double marginBottom = 56 * scaleY;

        double maxW = Math.max(320, vb.getWidth() - marginLeftRight);
        double maxH = Math.max(240, vb.getHeight() - marginBottom);

        if (stage.getWidth() > maxW) stage.setWidth(maxW);
        if (stage.getHeight() > maxH) stage.setHeight(maxH);

        double minX = vb.getMinX();
        double maxX = vb.getMaxX() - stage.getWidth();
        double minY = vb.getMinY() + marginTop;
        double maxY = vb.getMaxY() - marginBottom - stage.getHeight();

        if (stage.getX() < minX) stage.setX(minX);
        if (stage.getX() > maxX) stage.setX(maxX);
        if (stage.getY() < minY) stage.setY(minY);
        if (stage.getY() > maxY) stage.setY(maxY);
    }

    /**
     * На Windows иконка в панели задач может не меняться от {@link Stage#getIcons()}.
     * Поэтому дополнительно пытаемся задать иконку через AWT Taskbar (если поддерживается).
     */
    private void applyTaskbarIcon() {
        if (appIcon == null) return;
        try {
            if (!Taskbar.isTaskbarSupported()) return;
            Taskbar taskbar = Taskbar.getTaskbar();
            try (InputStream is = getClass().getResourceAsStream("/icons/app.png")) {
                if (is == null) return;
                java.awt.Image awtImage = ImageIO.read(is);
                if (awtImage == null) return;
                taskbar.setIconImage(awtImage);
            }
        } catch (Throwable ignored) {
            //в некоторых окружениях просто пропускаем
        }
    }

    public void showLoginWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setMainApp(this);
        applySceneAutoSize(primaryStage, root, "Lawyer Company System", true, View.LOGIN);
        primaryStage.setMinWidth(420);
        primaryStage.setMinHeight(320);
    }

    public void showRegisterWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register.fxml"));
        Parent root = loader.load();
        RegisterController controller = loader.getController();
        controller.setMainApp(this);
        applySceneAutoSize(primaryStage, root, "Lawyer Company System", true, View.REGISTER);
        primaryStage.setMinWidth(560);
        primaryStage.setMinHeight(520);
    }

    public void showClientDashboard(User user) throws Exception {
        //на всякий случай перезагружаем пользователя из БДшки
        this.currentUser = userService.getUserById(user.getId()).orElse(user);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client_dashboard.fxml"));
        Parent root = loader.load();
        ClientDashboardController controller = loader.getController();
        controller.setMainApp(this);
        controller.setCurrentUser(this.currentUser);
        applySceneAutoSize(primaryStage, root, "Lawyer Company System", true, View.CLIENT);
        primaryStage.setMinWidth(1030);
        primaryStage.setMinHeight(600);
        //это просто удобный дефолт, чтобы карточки адвокатов/таблица дел не выглядели тесно (пока что закомментировал)
        //primaryStage.setWidth(Math.max(primaryStage.getWidth(), 1180));
        //primaryStage.setHeight(Math.max(primaryStage.getHeight(), 760));
    }

    public void showCreateCaseWindow(User user, Lawyer preselectedLawyer) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/create_case.fxml"));
        Parent root = loader.load();
        CreateCaseController controller = loader.getController();
        controller.setMainApp(this);
        controller.setCurrentUser(userService.getUserById(user.getId()).orElse(user));
        controller.setPreselectedLawyer(preselectedLawyer);

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(primaryStage);
        applySceneAutoSize(stage, root, "Lawyer Company System", true);
        stage.setMinWidth(620);
        stage.setMinHeight(420);
        controller.setDialogStage(stage);
        stage.showAndWait();
    }

    public void refreshClientDashboard() {
        try {
            showClientDashboard(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshLawyerDashboard() {
        try {
            showLawyerDashboard(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showCaseWindow(Case caseEntity, User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/case_window.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(primaryStage);
        applySceneAutoSize(stage, root, "Lawyer Company System", true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        CaseWindowController controller = loader.getController();
        controller.setMainApp(this);
        controller.initData(caseEntity, userService.getUserById(user.getId()).orElse(user), stage);
        stage.showAndWait();  //модальное окно
    }

    public void showLawyerDashboard(User user) throws Exception {
        //фиксируем текущего пользователя, чтобы refreshLawyerDashboard() работал корректно
        this.currentUser = userService.getUserById(user.getId()).orElse(user);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/lawyer_dashboard.fxml"));
        Parent root = loader.load();
        LawyerDashboardController controller = loader.getController();
        controller.setMainApp(this);
        controller.setCurrentUser(this.currentUser);
        applySceneAutoSize(primaryStage, root, "Lawyer Company System", true, View.LAWYER);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
    }

    public void showAdminDashboard(User user) throws Exception {
        //фиксируем текущего пользователя, чтобы при refresh/логике UI было единообразно
        this.currentUser = userService.getUserById(user.getId()).orElse(user);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin_dashboard.fxml"));
        Parent root = loader.load();
        AdminDashboardController controller = loader.getController();
        controller.setMainApp(this);
        controller.setCurrentUser(this.currentUser);
        applySceneAutoSize(primaryStage, root, "LCS4admin", true, View.ADMIN);
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(620);
    }

    public static void main(String[] args) {
        initEarlyLogging();
        launch(args);
    }

    private static void initEarlyLogging() {
        try {
            Path logDir = Path.of(System.getProperty("user.home"), "LawyerCompanySystem", "logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("app.log");

            PrintStream ps = new PrintStream(
                    Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    true,
                    "UTF-8"
            );
            System.setOut(ps);
            System.setErr(ps);

            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                System.err.println("=== Uncaught exception in thread: " + t.getName() + " ===");
                e.printStackTrace();
            });

            System.out.println("=== App starting ===");
        } catch (Exception ignored) {
            //если запись лога недоступна, просто не падаем на старте
        }
    }
}
