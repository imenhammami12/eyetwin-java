package com.eyetwin;

public class Launcher {
    public static void main(String[] args) {
        // En appelant MainApp.main() depuis une classe qui n'étend pas Application,
        // on évite l'erreur classique "JavaFX runtime components are missing" 
        // lors de l'exécution sans système de modules (sans module-info.java).
        MainApp.main(args);
    }
}
