package ru.geekbrains.alekseiterentev;

public class MainApp {

    private final Object mon = new Object();
    private char currentLetter = 'A';

    public static void main(String[] args) {
        MainApp mainApp = new MainApp();

        new Thread(() -> {
            mainApp.printA();
        }).start();

        new Thread(() -> {
            mainApp.printB();
        }).start();

        new Thread(() -> {
            mainApp.printC();
        }).start();
    }

    public void printA() {
        synchronized (mon) {
            try {
                for (int i = 1; i < 5; i++) {
                    while (currentLetter != 'A') {
                        mon.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'B';
                    mon.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printB() {
        synchronized (mon) {
            try {
                for (int i = 1; i < 5; i++) {
                    while (currentLetter != 'B') {
                        mon.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'C';
                    mon.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printC() {
        synchronized (mon) {
            try {
                for (int i = 1; i < 5; i++) {
                    while (currentLetter != 'C') {
                        mon.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'A';
                    mon.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
