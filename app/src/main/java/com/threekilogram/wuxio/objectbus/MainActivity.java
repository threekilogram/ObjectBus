package com.threekilogram.wuxio.objectbus;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.threekilogram.objectbus.bus.ObjectBus;
import com.threekilogram.objectbus.bus.ObjectBus.Predicate;
import com.threekilogram.objectbus.runnable.EchoRunnable;

/**
 * @author liujin
 */
public class MainActivity extends AppCompatActivity {

      private static final String TAG = MainActivity.class.getSimpleName();
      private ConstraintLayout mRoot;

      private ObjectBus mObjectBus;

      @Override
      protected void onCreate ( Bundle savedInstanceState ) {

            super.onCreate( savedInstanceState );
            setContentView( R.layout.activity_main );
            initView();
            mObjectBus = ObjectBus.newList();

            test();
      }

      private void initView ( ) {

            mRoot = (ConstraintLayout) findViewById( R.id.root );
      }

      public void addMain ( View view ) {

            mObjectBus.toMain( new MainRunnable() );
      }

      public void toPool ( View view ) {

            mObjectBus.toPool( new MainRunnable() );
      }

      public void toMainDelayed ( View view ) {

            mObjectBus.toMain( 2000, new MainRunnable() );
      }

      public void toPoolDelayed ( View view ) {

            mObjectBus.toPool( 2000, new MainRunnable() );
      }

      public void run ( View view ) {

            mObjectBus.run();
      }

      public void size ( View view ) {

            int i = mObjectBus.remainSize();
            log( String.valueOf( i ) );
      }

      private static void log ( String msg ) {

            Log.e(
                TAG,
                "log : "
                    + msg + " "
                    + System.currentTimeMillis() + " "
                    + Thread.currentThread().getName()
            );
      }

      public void isRunning ( View view ) {

            log( Boolean.toString( mObjectBus.isRunning() ) );
      }

      public void pause ( View view ) {

            mObjectBus.pause();
      }

      public void resume ( View view ) {

            mObjectBus.resume();
      }

      public void clearAll ( View view ) {

            mObjectBus.cancelAll();
      }

      public void clearOne ( View view ) {

            MainRunnable runnable = new MainRunnable();
            mObjectBus.toPool( runnable );
            mObjectBus.cancel( runnable );
            log( "cancel " + runnable );
      }

      public void list ( View view ) {

            if( mObjectBus != null ) {
                  mObjectBus.cancelAll();
            }
            mObjectBus = ObjectBus.newList();
      }

      public void queue ( View view ) {

            if( mObjectBus != null ) {
                  mObjectBus.cancelAll();
            }
            mObjectBus = ObjectBus.newQueue();
      }

      public void fixSize ( View view ) {

            if( mObjectBus != null ) {
                  mObjectBus.cancelAll();
            }
            mObjectBus = ObjectBus.newFixSizeQueue( 3 );
            mObjectBus = ObjectBus.newFixSizeList( 3 );
      }

      public void ifFalseFalse ( View view ) {

            mObjectBus.ifFalse( new Predicate() {

                  @Override
                  public boolean test ( ObjectBus bus ) {

                        return false;
                  }
            } );
      }

      public void ifTrueFalse ( View view ) {

            mObjectBus.ifTrue( new Predicate() {

                  @Override
                  public boolean test ( ObjectBus bus ) {

                        return false;
                  }
            } );
      }

      private class MainRunnable implements Runnable {

            @Override
            public void run ( ) {

                  log( this.toString() );
            }
      }

      public void test ( ) {

//            mObjectBus.toPool( new Runnable() {
//
//                  @Override
//                  public void run ( ) {
//
//                        mObjectBus.setResult( "result", "Hello" );
//                  }
//            } ).ifTrue( new Predicate() {
//
//                  @Override
//                  public boolean test ( ObjectBus bus ) {
//
//                        return false;
//                  }
//            } ).toMain( new Runnable() {
//
//                  @Override
//                  public void run ( ) {
//
//                        String result = mObjectBus.getResultOff( "result" );
//                  }
//            } ).run();
//
//            if( mObjectBus.isRunning() ) {
//                  mObjectBus.pause();
//            }
//
//            if( mObjectBus.isPaused() ) {
//                  mObjectBus.resume();
//            }
//
//            mObjectBus.toPool( 1500, new Runnable() {
//
//                  @Override
//                  public void run ( ) {
//
//                  }
//            } ).toMain( 1500, new Runnable() {
//
//                  @Override
//                  public void run ( ) {
//
//                  }
//            } ).run();
//
//            mObjectBus.toMain( new Executable() {
//
//                  @Override
//                  public void onStart ( ) {
//
//                  }
//
//                  @Override
//                  public void onExecute ( ) {
//
//                  }
//
//                  @Override
//                  public void onFinish ( ) {
//
//                  }
//            } ).run();

            mObjectBus.toPool( new EchoRunnable() {

                  @Override
                  protected void onResult ( Object result ) {

                        Log.e(
                            TAG, "onResult : " + result + " " + Thread.currentThread().getName() );
                  }

                  @Override
                  public void run ( ) {

                        setResult( "Hello Echo 01" );
                  }
            } ).toPool( new EchoRunnable() {

                  @Override
                  protected void onResult ( Object result ) {

                        Log.e(
                            TAG, "onResult : " + result + " " + Thread.currentThread().getName() );
                  }

                  @Override
                  public void run ( ) {

                        setResult( "Hello Echo 02" );
                  }
            } ).run();
      }
}