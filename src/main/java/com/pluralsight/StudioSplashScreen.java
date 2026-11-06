package com.pluralsight;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;

public class StudioSplashScreen {

    public static void show(Stage ownerStage, Runnable onFinished) {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);
        if (ownerStage != null) {
            splashStage.initOwner(ownerStage);
        }

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #000000; -fx-background-radius: 16; -fx-border-color: #d4af37; -fx-border-radius: 16; -fx-border-width: 2; -fx-padding: 30;");

        // Load Logo Image
        Image logoImg = loadLogoImage();
        ImageView logoView = new ImageView();
        if (logoImg != null) {
            logoView.setImage(logoImg);
            logoView.setFitWidth(220);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
        }

        Text titleText = new Text("VISUAL TIZITA");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        titleText.setFill(Color.web("#d4af37"));

        Text tagText = new Text("Memories that last forever");
        tagText.setFont(Font.font("Segoe UI", 12));
        tagText.setFill(Color.web("#9ca3af"));

        Text loadingText = new Text("Loading Studio Suite & Ledger...");
        loadingText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        loadingText.setFill(Color.web("#10b981"));

        root.getChildren().addAll(logoView, titleText, tagText, loadingText);

        // Start fully transparent for cinematic fade-in
        root.setOpacity(0.0);

        Scene scene = new Scene(root, 420, 320);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        // Cinematic Animations: Fade In + Subtle Scale Up (Total duration: 2500ms / 2.5 seconds)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
        fadeIn.setToValue(1.0);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(1600), root);
        scaleUp.setFromX(0.92);
        scaleUp.setFromY(0.92);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), root);
        fadeOut.setDelay(Duration.millis(1400));
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            splashStage.close();
            onFinished.run();
        });

        SequentialTransition sequence = new SequentialTransition(
                new javafx.animation.ParallelTransition(fadeIn, scaleUp),
                fadeOut
        );
        sequence.play();
    }

    private static Image loadLogoImage() {
        String[] candidatePaths = {"logo.jpg", "Visual Tizita Logo 2.jpg", "src/main/resources/logo.jpg", "src/main/resources/Visual Tizita Logo 2.jpg"};
        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists()) return new Image(f.toURI().toString());
        }
        InputStream stream = StudioSplashScreen.class.getResourceAsStream("/logo.jpg");
        return stream != null ? new Image(stream) : null;
    }
}