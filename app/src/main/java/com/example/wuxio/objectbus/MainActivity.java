package com.example.wuxio.objectbus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.objectbus.bus.ObjectBus;
import com.example.objectbus.bus.OnAfterRunAction;
import com.example.objectbus.bus.OnBeforeRunAction;
import com.example.objectbus.executor.AppExecutor;
import com.example.objectbus.executor.OnExecuteRunnable;
import com.example.objectbus.message.Messengers;
import com.example.objectbus.message.OnMessageReceiveListener;
import com.example.objectbus.schedule.CancelTodo;
import com.example.objectbus.schedule.Scheduler;
import com.example.objectbus.schedule.run.AsyncThreadCallBack;
import com.example.objectbus.schedule.run.MainThreadCallBack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author wuxio
 */
public class MainActivity extends AppCompatActivity implements OnMessageReceiveListener {

    private static final String TAG = "MainActivity";
    protected NavigationView mNavigationView;
    protected DrawerLayout   mDrawerLayout;
    protected TextView       mTextLog;
    protected ScrollView     mContainer;

    private String mLog = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MainManager.getInstance().register(this);

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
    }


    private void initView() {

        mNavigationView = findViewById(R.id.navigationView);
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mTextLog = findViewById(R.id.text_log);
        mContainer = findViewById(R.id.container);

        mNavigationView.setNavigationItemSelectedListener(new MainNavigationItemClick());
    }


    private void closeDrawer() {

        mDrawerLayout.closeDrawer(Gravity.START);
    }


    public synchronized static void print(String text) {

        String msg = ":" +
                " Thread: " + Thread.currentThread().getName() +
                " time: " + System.currentTimeMillis() +
                " msg: " + text;
        Log.i(TAG, msg);

    }


    private ObjectBus mLogBus = new ObjectBus();


    private void clearLogText() {

        mLog = "";
        mTextLog.setText("");
    }


    private synchronized void printLog(String log) {

        String s = log + " : " +
                " ThreadOn: " + Thread.currentThread().getName() + ";" +
                " timeAt: " + System.currentTimeMillis() + "\n";
        mLog = mLog + s;

        mLogBus.toMain(() -> {
            mTextLog.setText(mLog);
            mLogBus.clearRunnable();
        }).run();
    }


    private synchronized void printText(String text) {

        mLog = mLog + text;

        mLogBus.toMain(() -> {
            mTextLog.setText(mLog);
            mLogBus.clearRunnable();
        }).run();
    }


    private void addEnter() {

        mLog = mLog + "\n";
    }


    class MainNavigationItemClick implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            clearLogText();

            switch (item.getItemId()) {

                /* Scheduler */

                case R.id.menu_00:
                    testSchedulerTodo();
                    break;
                case R.id.menu_01:
                    testSchedulerTodoDelayed();
                    break;
                case R.id.menu_02:
                    testSchedulerTodoMainCallBack();
                    break;
                case R.id.menu_03:
                    testSchedulerTodoAsyncCallBack();
                    break;
                case R.id.menu_04:
                    testSchedulerTodoWithListener();
                    break;
                case R.id.menu_05:
                    testSchedulerCancel();
                    break;

                /* AppExecutor */

                case R.id.menu_06:
                    testExecutorRunnable();
                    break;
                case R.id.menu_07:
                    testExecutorCallable();
                    break;
                case R.id.menu_08:
                    testExecutorCallableAndGet();
                    break;
                case R.id.menu_09:
                    testExecutorRunnableList();
                    break;
                case R.id.menu_10:
                    testExecutorCallableList();
                    break;

                /* Messenger */

                case R.id.menu_11:
                    testMessengerSend();
                    break;
                case R.id.menu_12:
                    testMessengerSendDelayed();
                    break;
                case R.id.menu_13:
                    testMessengerSendWithExtra();
                    break;
                case R.id.menu_14:
                    testMessengerRemove();
                    break;

                /* ObjectBus */

                case R.id.menu_15:
                    testBusGo();
                    break;
                case R.id.menu_16:
                    testBusGoWithParams();
                    break;
                case R.id.menu_17:
                    testBusTakeRest();
                    break;
                case R.id.menu_18:
                    testBusStopRest();
                    break;

                default:
                    break;
            }

            closeDrawer();
            return true;
        }
    }

    //============================ AppExecutor ============================


    public void testExecutorRunnable() {

        AppExecutor.execute(new Runnable() {
            @Override
            public void run() {

                printLog(" run ");
                print(" run ");
            }
        });
    }


    public void testExecutorCallable() {

        Future< String > submit = AppExecutor.submit(new Callable< String >() {
            @Override
            public String call() throws Exception {

                String s = " call At";
                printLog(s);
                print(s);
                return "Hello";
            }
        });

        /* 不要在主线程{submit.get()},否则主线程会阻塞 */

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                try {
                    String s = submit.get();
                    printLog(" getAt: " + s);
                    print(" getAt: " + s);

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void testExecutorCallableAndGet() {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                /* submitAndGet 会阻塞调用线程,推荐和Scheduler配合,在后台读取结果 */

                String get = AppExecutor.submitAndGet(new Callable< String >() {
                    @Override
                    public String call() throws Exception {

                        String s = "Hello";
                        printLog(s);
                        print(s);
                        return s;
                    }
                });

            }
        });
    }


    public void testExecutorRunnableList() {

        final int size = 4;
        List< Runnable > runnableList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {

            final int j = i;

            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    String s = "running " + j;
                    printLog(s);
                    print(s);
                }
            };
            runnableList.add(runnable);
        }

        AppExecutor.execute(runnableList);
    }


    public void testExecutorCallableList() {

        /* 因为 submitAndGet 会阻塞调用线程,所以和Scheduler配合,在后台读取结果 */

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                final int size = 4;
                List< Callable< String > > callableList = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {

                    final int j = i;
                    Callable< String > callable = new Callable< String >() {
                        @Override
                        public String call() throws Exception {

                            String s = " calling " + j;
                            printLog(s);
                            print(s);
                            return "Hello " + j;
                        }
                    };

                    callableList.add(callable);
                }

                List< String > stringList = AppExecutor.submitAndGet(callableList);

                int length = stringList.size();
                for (int i = 0; i < length; i++) {
                    String s = stringList.get(i);
                    printLog(s);
                    print(s);
                }

                addEnter();
                String s = "可以看出 callable 运行在其他线程;结果都在同一个线程读取";
                printText(s);
            }
        });
    }

    //============================ scheduler ============================


    public void testSchedulerTodo() {


        /* 以下会将任务带到后台执行  */

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                printLog(" todo 01 ");
                print(" todo 01 ");
            }
        });

        /* lambda 简化编程 */

        Scheduler.todo(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            printLog(" todo 02 ");
            print(" todo 02 ");
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                printLog(" todo 03 ");
                print(" todo 03 ");
            }
        });
    }


    public void testSchedulerTodoDelayed() {

        Scheduler.todo(() -> {

            String msg = " delayed01 ";
            printLog(msg);
            print(msg);
        }, 1000);

        Scheduler.todo(() -> {

            String msg = " delayed02 ";
            printLog(msg);
            print(msg);

        }, 2000);

        Scheduler.todo(() -> {

            String msg = " delayed03 ";
            printLog(msg);
            print(msg);
        }, 3000);

        Scheduler.todo(() -> {

            String msg = " delayed04 ";
            printLog(msg);
            print(msg);
        }, 4000);

        Scheduler.todo(() -> {

            String msg = " delayed05 ";
            printLog(msg);
            print(msg);
        }, 5000);
    }


    public void testSchedulerTodoMainCallBack() {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                String msg = "back";
                printLog(msg);
                print(msg);
            }
        }, new MainThreadCallBack() {
            @Override
            public void run() {

                String msg = "callback";
                printLog(msg);
                print(msg);
                addEnter();
            }
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                String msg = "back delayed";
                printLog(msg);
                print(msg);
            }
        }, 1000, new MainThreadCallBack() {
            @Override
            public void run() {

                String msg = "callback";
                printLog(msg);
                print(msg);
            }
        });
    }


    public void testSchedulerTodoAsyncCallBack() {

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                String msg = "back";
                printLog(msg);
                print(msg);
            }
        }, new AsyncThreadCallBack() {
            @Override
            public void run() {

                String msg = "callback";
                printLog(msg);
                print(msg);
                addEnter();
            }
        });

        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                String msg = "back delayed";
                printLog(msg);
                print(msg);
            }
        }, 1000, new AsyncThreadCallBack() {
            @Override
            public void run() {

                String msg = "callback";
                printLog(msg);
                print(msg);
            }
        });
    }


    private boolean mFlag;


    public void testSchedulerTodoWithListener() {

        Scheduler.todo(new OnExecuteRunnable() {

            @Override
            public void onStart() {

                String msg = "task start";
                printLog(msg);
                print(msg);
            }


            @Override
            public void onExecute() {

                String msg = "task running";
                printLog(msg);
                print(msg);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mFlag = !mFlag;
                if (mFlag) {
                    throw new RuntimeException("null");
                }
            }


            @Override
            public void onFinish() {

                String msg = "task finish";
                printLog(msg);
                print(msg);
            }


            @Override
            public void onException(Exception e) {

                String msg = "exception";
                printLog(msg);
                print(msg);
            }
        });
    }


    public void testSchedulerCancel() {

        CancelTodo cancelTodo = new CancelTodo();
        Scheduler.todo(new Runnable() {
            @Override
            public void run() {

                String msg = "running";
                printLog(msg);
                print(msg);
            }
        }, 2000, cancelTodo);

        if (mFlag) {
            cancelTodo.cancel();
            Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
        } else {
            String s = "2s 后";
            printText(s);
            addEnter();
            print(s);
        }

        mFlag = !mFlag;
    }

    /* 实现 OnMessageReceiveListener, 以接收消息 */


    @Override
    public void onReceive(int what, Object extra) {

        String s = "MainActivity receive: " + what + " extra: " + extra;
        printLog(s);
        print(s);
        addEnter();
    }


    @Override
    public void onReceive(int what) {

        String s = "MainActivity receive: " + what;
        printLog(s);
        print(s);
        addEnter();
    }


    /* 如果一个类实现不了 OnMessageReceiveListener 接口,使用如下包装者模式,以实现通信 */

    private MessengerReceiver mMessengerReceiver = new MessengerReceiver(this);

    private static class MessengerReceiver implements OnMessageReceiveListener {


        private WeakReference< MainActivity > mReference;


        public MessengerReceiver(MainActivity activity) {

            mReference = new WeakReference<>(activity);
        }


        @Override
        public void onReceive(int what, Object extra) {

            /* try catch 因为 mReference.get() 可能会为null */

            try {
                String s = "MessengerReceiver receive: " + what + " extra: " + extra;
                mReference.get().printLog(s);
                print(s);
                mReference.get().addEnter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onReceive(int what) {

            /* try catch 因为 mReference.get() 可能会为null */

            try {
                String s = "MessengerReceiver receive: " + what;
                mReference.get().printLog(s);
                print(s);
                mReference.get().addEnter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //============================ test messenger ============================


    public void testMessengerSend() {


        /* message what 的奇偶性决定发送到哪个线程 */

        /* 1 is odd number,the message will send to main thread */

        Messengers.send(1, this);

        /* 2 is even number, the message will send to a Messenger thread*/

        Messengers.send(2, this);

        Messengers.send(1, mMessengerReceiver);
        Messengers.send(2, mMessengerReceiver);
    }


    public void testMessengerSendDelayed() {

        String s = "send delayed message";
        printText(s);
        printLog(s);
        addEnter();

        Messengers.send(3, 2000, this);
        Messengers.send(4, 2000, this);

        Messengers.send(3, 2000, mMessengerReceiver);
        Messengers.send(4, 2000, mMessengerReceiver);
    }


    public void testMessengerSendWithExtra() {

        String s = "send message with extra";
        printText(s);
        printLog(s);
        addEnter();

        Messengers.send(5, " hello ", this);
        Messengers.send(6, " hello ", this);

        Messengers.send(7, 2000, " hello ", this);
        Messengers.send(8, 2000, " hello ", this);

        Messengers.send(5, " hello ", mMessengerReceiver);
        Messengers.send(6, " hello ", mMessengerReceiver);

        Messengers.send(7, 2000, " hello ", mMessengerReceiver);
        Messengers.send(8, 2000, " hello ", mMessengerReceiver);
    }


    public void testMessengerRemove() {

        Messengers.send(9, 2000, " hello ", this);

        Messengers.send(9, 2000, " hello mainManager ", mMessengerReceiver);

        if (mFlag) {
            Messengers.remove(9, this);
            Toast.makeText(this, "removed", Toast.LENGTH_SHORT).show();
        } else {
            String s = " 2s 后收到消息 ";
            printText(s);
            addEnter();
            print(s);
        }

        mFlag = !mFlag;
    }

    //============================ mBus ============================

    private boolean running = false;


    public void testBusGo() {

        ObjectBus bus = new ObjectBus();

        bus.go(() -> {
            String s = "task 01";
            printLog(s);
            print(s);
        }).toUnder(() -> {

            String s = "task 02";
            printLog(s);
            print(s);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).go(() -> {

            String s = "task 03";
            printLog(s);
            print(s);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).toMain(() -> {

            String s = "task 04";
            printLog(s);
            print(s);

            addEnter();
            printText("在不同的线程上顺次执行");

        }).run();
    }


    public void testBusGoWithParams() {

        ObjectBus bus = new ObjectBus();

        bus.go(() -> {

            int j = 99 + 99;
            String s = "take " + j;
            printLog(s);
            print(s);
            addEnter();

            bus.take(j, "result");

        }).toUnder(() -> {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Integer result = (Integer) bus.get("result");

            String s = "get from last Task " + result;
            printLog(s);
            print(s);
            addEnter();

            int k = result + 1002;
            bus.take(k, "result");

        }).go(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Integer result = (Integer) bus.get("result");
            String s = "get from last Task " + result;
            printLog(s);
            print(s);
            addEnter();

            int l = result + 3000;
            bus.take(l, "result");

        }).toMain(() -> {

            Integer result = (Integer) bus.get("result");
            String s = "get final result " + result;
            printLog(s);
            print(s);

        }).run();

    }


    ObjectBus mBus = new ObjectBus();


    public void testBusTakeRest() {

        mBus.go(new Runnable() {

            @Override
            public void run() {

                String s = "do task 01";
                printLog(s);
                print(s);

                addEnter();

                printText("take rest; wait for bus.stopRest()");

            }

        }).takeRest()
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print(" after take a rest go on do task 02 ");

                    }
                }).run();

    }


    public void testBusStopRest() {

        mBus.stopRest();
    }


    public void testBusMessage(View view) {

        mBus.go(new Runnable() {
            @Override
            public void run() {

                print(" do someThing  ");
            }
        }).send(158, mBus, MainManager.getInstance())
                .takeRest()
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print(" rest finished ");
                    }
                })
                .run();

    }


    public void testBusMessageRegister(View view) {

        mBus.go(new Runnable() {
            @Override
            public void run() {

                print(" do someThing  ");
            }
        }).registerMessage(88, new Runnable() {
            @Override
            public void run() {

                print(" receive message 88");
            }
        }).registerMessage(87, new Runnable() {
            @Override
            public void run() {

                print(" receive message 87");
            }
        }).go(new Runnable() {
            @Override
            public void run() {

                print(" do finished ");

            }
        }).run();

        Messengers.send(88, 3000, mBus);
        Messengers.send(87, 3000, mBus);

    }


    public void testBusCallableList(View view) {

        ObjectBus bus = new ObjectBus();

        List< Callable< String > > callableList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

            int j = i;

            Callable< String > callable = new Callable< String >() {
                @Override
                public String call() throws Exception {

                    Thread.sleep(1000);

                    return String.valueOf(j);
                }
            };

            callableList.add(callable);

        }

        bus.toUnder(callableList, "CAll_LIST")
                .go(new Runnable() {
                    @Override
                    public void run() {

                        List< String > result = (List< String >) bus.get("CAll_LIST");

                        Log.i(TAG, "run:" + result);
                    }
                }).run();

    }


    public void testBusCallable(View view) {

        ObjectBus bus = new ObjectBus();

        Callable< String > callable = new Callable< String >() {
            @Override
            public String call() throws Exception {

                Thread.sleep(1000);

                return String.valueOf(1990);
            }
        };

        bus.toUnder(callable, "CAll")
                .go(new Runnable() {
                    @Override
                    public void run() {

                        String result = (String) bus.get("CAll");

                        Log.i(TAG, "run:" + result);
                    }
                }).run();

    }


    public void testBusRunnableList(View view) {

        ObjectBus bus = new ObjectBus();

        List< Runnable > runnableList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    try {
                        print("start");
                        Thread.sleep(1000);
                        print("end");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            runnableList.add(runnable);

        }

        bus.toUnder(runnableList)
                .go(new Runnable() {
                    @Override
                    public void run() {

                        print("all finished");
                    }
                }).run();

    }


    public void testBusLazyGo(View view) {

        ObjectBus bus = new ObjectBus();

        bus.go(new Runnable() {
            @Override
            public void run() {

                print(" do something 01 ");
            }
        }).go(new OnBeforeRunAction< Runnable >() {
            @Override
            public void onBeforeRun(Runnable runnable) {

                print("before take to mBus" + runnable);
                bus.take("Hello runnable", "key");
            }
        }, new Runnable() {
            @Override
            public void run() {

                String msg = (String) bus.get("key");

                print(msg + " get from mBus ");
            }
        }, new OnAfterRunAction< Runnable >() {
            @Override
            public void onAfterRun(Object bus, Runnable runnable) {

                print("after run " + runnable);

            }
        }).run();
    }
}
