package com.example.lukas.euglenapatterns;


import android.graphics.drawable.Drawable;

public class PatternSwitchThread extends Thread {

    private PresentationService presentationService;
    private Drawable patternOne;
    private Drawable patternTwo;

    public PatternSwitchThread(PresentationService service, Drawable drawOne, Drawable drawTwo) {
        presentationService = service;
        patternOne = drawOne;
        patternTwo = drawTwo;
    }

    @Override
    public void run() {

        // Loop continuously, writing data, until thread.interrupt() is called
        while (!this.isInterrupted()) {
            presentationService.updatePattern(patternOne);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            presentationService.updatePattern(patternTwo);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
