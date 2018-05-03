package com.example.objectbus.bus;

import android.support.annotation.NonNull;

import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.Scheduler;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wuxio 2018-05-03:16:16
 */
public class ObjectBus {

    private static final int GO       = 0b1;
    private static final int TO_UNDER = 0b10;
    private static final int TO_MAIN  = 0b100;
    private static final int SEND     = 0b1000;


    private static final int MAIN_THREAD     = 0X1FFFF;
    private static final int EXECUTOR_THREAD = 0X2FFFF;
    private int currentThread;

    /**
     * how many station pass By
     */
    private AtomicInteger        mPassBy    = new AtomicInteger();
    private ArrayList< Command > mHowToPass = new ArrayList<>();

    private BusOnExecuteRunnable mBusOnExecuteRunnable = new BusOnExecuteRunnable();
    private BusMessageListener   mBusMessageListener   = new BusMessageListener();


    public ObjectBus() {

    }


    /**
     * @return hoe many station pass by
     */
    public int getPassBy() {

        return mPassBy.get();
    }


    /**
     * to next station
     */
    private void toNextStation() {

        int index = mPassBy.getAndAdd(1);
        if (index < mHowToPass.size()) {

            Command command = mHowToPass.get(index);
            doCommand(command);
        } else {

            // TODO: 2018-05-03 how to set current value

            //mPassBy.set(0);
        }
    }


    /**
     * @param command use command to run runnable
     */
    private void doCommand(Command command) {

        if (command.command == GO) {
            command.run();
            toNextStation();
            return;
        }

        if (command.command == TO_UNDER) {
            if (currentThread != EXECUTOR_THREAD) {
                mBusOnExecuteRunnable.setRunnable(command.getRunnable());
                Scheduler.todo(mBusOnExecuteRunnable);
                currentThread = EXECUTOR_THREAD;
            } else {
                command.run();
                toNextStation();
            }
            return;
        }

        if (command.command == TO_MAIN) {
            if (currentThread != MAIN_THREAD) {
                BusMessageListener messenger = mBusMessageListener;
                messenger.setRunnable(command.getRunnable());
                messenger.runOnMain();
                currentThread = MAIN_THREAD;
            } else {
                command.run();
                toNextStation();
            }

            return;
        }

        if (command.command == SEND) {
            command.run();
            toNextStation();
            return;
        }
    }


    /**
     * run runnable on current thread;
     * if call {@link #toUnder(Runnable)} current thread will be
     * {@link com.example.objectbus.executor.AppExecutor} thread;
     * if call {@link #toMain(Runnable)} current thread will be main thread;
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus go(@NonNull Runnable task) {

        mHowToPass.add(new Command(GO, task));
        return this;
    }


    /**
     * run runnable on {@link com.example.objectbus.executor.AppExecutor} thread
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus toUnder(@NonNull Runnable task) {

        mHowToPass.add(new Command(TO_UNDER, task));
        return this;
    }


    /**
     * run runnable on main thread
     *
     * @param task task to run
     * @return self
     */
    public ObjectBus toMain(@NonNull Runnable task) {

        mHowToPass.add(new Command(TO_MAIN, task));
        return this;
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus send(int what, OnMessageReceiveListener listener) {

        return sendDelayed(what, 0, null, listener);
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param extra    extra msg
     * @param listener receiver
     * @return self
     */
    public ObjectBus send(int what, Object extra, OnMessageReceiveListener listener) {

        return sendDelayed(what, 0, extra, listener);
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param listener receiver
     * @return self
     */
    public ObjectBus sendDelayed(int what, int delayed, OnMessageReceiveListener listener) {

        return sendDelayed(what, delayed, null, listener);
    }


    /**
     * send message on current thread,same as {@link #go(Runnable)}
     *
     * @param what     message what
     * @param extra    extra msg
     * @param listener receiver
     * @return self
     */
    public ObjectBus sendDelayed(int what, int delayed, Object extra, OnMessageReceiveListener listener) {

        mHowToPass.add(new Command(SEND, new SendRunnable(what, delayed, extra, listener)));
        return this;
    }


    /**
     * start run bus
     */
    public void run() {

        toNextStation();
    }

    //============================ command for Bus run runnable ============================

    /**
     * record how to run runnable
     */
    private class Command {

        private int      command;
        private Runnable mRunnable;


        Command(int command, @NonNull Runnable runnable) {

            this.command = command;
            mRunnable = runnable;
        }


        void run() {

            mRunnable.run();
        }


        Runnable getRunnable() {

            return mRunnable;
        }
    }

    //============================ executor runnable  ============================

    /**
     * use take task to do in the {@link com.example.objectbus.executor.AppExecutor}
     */
    private class BusOnExecuteRunnable implements OnExecuteRunnable {

        private Runnable mRunnable;


        void setRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        @Override
        public void onExecute() {

            Runnable runnable = mRunnable;
            if (runnable != null) {
                runnable.run();
            }
        }


        @Override
        public void onFinish() {

            toNextStation();
        }


        @Override
        public void onException(Exception e) {

            // TODO: 2018-05-03 how to handle exception

        }
    }

    //============================ main runnable ============================

    private class BusMessageListener implements OnMessageReceiveListener {

        private static final int WHAT_MAIN = 3;

        private Runnable mRunnable;


        void setRunnable(Runnable runnable) {

            mRunnable = runnable;
        }


        void runOnMain() {

            Messengers.send(WHAT_MAIN, this);
        }


        @Override
        public void onReceive(int what) {

            mRunnable.run();
            toNextStation();
        }
    }

    //============================ Send runnable ============================

    private class SendRunnable implements Runnable {

        private int                      what;
        private int                      delayed;
        private Object                   extra;
        private OnMessageReceiveListener receiveListener;


        SendRunnable(int what, int delayed, Object extra, @NonNull OnMessageReceiveListener receiveListener) {

            this.what = what;
            this.extra = extra;
            this.delayed = delayed;
            this.receiveListener = receiveListener;
        }


        @Override
        public void run() {

            if (extra == null) {

                Messengers.send(what, delayed, receiveListener);
            } else {

                Messengers.send(what, delayed, extra, receiveListener);
            }
        }
    }
}
