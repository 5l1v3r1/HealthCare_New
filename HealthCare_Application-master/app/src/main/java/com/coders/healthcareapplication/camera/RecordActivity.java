package com.coders.healthcareapplication.camera;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coders.healthcareapplication.R;
import com.coders.healthcareapplication.view.MainActivity;
import com.coders.healthcareapplication.view.PopupRecordActivity;
import com.orbbec.astra.Astra;
import com.orbbec.astra.Body;
import com.orbbec.astra.BodyFrame;
import com.orbbec.astra.BodyStream;
import com.orbbec.astra.ColorFrame;
import com.orbbec.astra.ColorStream;
import com.orbbec.astra.ImageStreamMode;
import com.orbbec.astra.PixelFormat;
import com.orbbec.astra.ReaderFrame;
import com.orbbec.astra.StreamReader;
import com.orbbec.astra.StreamSet;
import com.orbbec.astra.android.AstraAndroidContext;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class RecordActivity extends AppCompatActivity {

    private Executor ex;
    private SurfaceView camView;
    private SurfaceHolder camHolder;
    private TextView feedbackView;

    private RGBData rgbData;
    private ExerFileController efc;
    private BodyData bodyData;

    private boolean thread_stop;

    private AstraAndroidContext aac;

    private int count;
    private int second;

    private Button back;
    private Button btn_dbinfo_convert_to_normal;
    private Button record_start;

    private ProgressBar recordProgressbar;

    private View decorView;
    private int	uiOption;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
            uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility( uiOption );

        camView = (SurfaceView) findViewById(R.id.admin_cam);
        camHolder = camView.getHolder();

        feedbackView = (TextView) findViewById(R.id.admin_feedback);

        rgbData = new RGBData();
        bodyData = new BodyData(rgbData.getNUMBEROFFRAME());
        efc = new ExerFileController();

        recordProgressbar = (ProgressBar)findViewById(R.id.recordProgressbar);

        /*check button instance*/
        record_start=(Button)findViewById(R.id.btn_record_start);
        count=0;
//sero 480 garo 640
        /*make event listenr*/
        record_start.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                        if(count==0) {

                            show_dialog();

                            //record start


                        }
                        else if(count==1)
                        {
                            record_start.setText("녹화");
                            //record end
                            count=0;
                            Intent intentToPupRecord=new Intent(getApplicationContext(), PopupRecordActivity.class);
                            //intent.putExtra("exercisename",ContentListAdapter.exercises.get(position));
                            startActivity(intentToPupRecord);
                        }

                    }
                }
        );


        /*check button instance*/
        back=(Button)findViewById(R.id.btn_back_record);
        /*make event listenr*/
        back.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                        onBackPressed();
                        finish();
                    }
                }
        );

        /*check button instance*/
        btn_dbinfo_convert_to_normal=(Button)findViewById(R.id.btn_convert_to_normal_record);
        /*make event listenr*/
        btn_dbinfo_convert_to_normal.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                        Log.i("Act.btn_convert2Ad_List","onClick");
                        /*인텐트 생성 후 명시적 다음 액티비티 호출*/
                        Intent intent1=new Intent(RecordActivity.this, MainActivity.class);
                        startActivity(intent1);
                        finish();
                    }
                }
        );
    }

    @Override
    protected void onResume(){
        super.onResume();

        aac = new AstraAndroidContext(getApplicationContext());
        aac.initialize();
        aac.openAllDevices();

        thread_stop = false;

        second = 0;
    }

    @Override
    protected void onPause(){
        super.onPause();

        thread_stop = true;
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        // super.onWindowFocusChanged(hasFocus);

        if( hasFocus ) {
            decorView.setSystemUiVisibility( uiOption );
        }
    }
    Handler mHandler = new Handler();
    void show_dialog()
    {
        final EditText edittext = new EditText(this);
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
                public void onFocusChange(View view, boolean b) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(edittext, 0);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("동영상 길이");
        builder.setMessage("원하는 동영상 길이를 입력하세요 (초기준)");
        builder.setView(edittext);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(),edittext.getText().toString() ,Toast.LENGTH_LONG).show();
                        second=Integer.parseInt(edittext.getText().toString());
                        record_start.setText("녹화 중");
                        thread_stop = false;

                        feedbackStr = "카메라 셋팅 중";
                        textHandler.sendMessage(textHandler.obtainMessage());

                        // Executor class
                        ex = new Executor(){
                            @Override
                            public void execute(@NonNull Runnable r) {
                                new Thread (r).start();
                            }
                        };
                        // Execute the Runnable object
                        ex.execute(new RecordActivity.UpdateRunnable());
                        count=1;


                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

      //  AlertDialog dialog = builder.create();

      /*  mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(edittext, 0);
            }
        },1000);*/

        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
       // dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        builder.show();
    }

    /* 피드백 텍스트뷰 핸들러 */
    private String feedbackStr = "";
    final Handler textHandler = new Handler(){
        public void handleMessage(Message msg){
            feedbackView.setText(feedbackStr);
        }
    };

    /* 프로그래스바 핸들러 */
    private int recordProgressPercent;
    final Handler recordProgressHandler = new Handler(){
        public void handleMessage(Message msg){
            recordProgressbar.setProgress(recordProgressPercent);
        }
    };

    /* 실제 구동 소스 코드 */
    private class UpdateRunnable implements Runnable {
        int cnt = 0;
        int nowScore = 0;
        int isBodyTracking = 0;

        @Override
        public void run() {
            try {
                while(second == 0);
                rgbData.setNUMBEROFFRAME(second);

                feedbackStr = "사용자의 신체를 탐색 중 (신체 탐색이 완료되고 1초 뒤부터 자세 촬영 시작)";
                textHandler.sendMessage(textHandler.obtainMessage());

                efc.openLoggingFile();
                efc.openBodyFile_W();
                efc.openRGBFile_W();

                StreamSet streamSet = StreamSet.open();
                StreamReader reader = streamSet.createReader();

                ColorStream colorStream = ColorStream.get(reader);
                Iterator ismi = colorStream.getAvailableModes().iterator();
                while(ismi.hasNext()){
                    ImageStreamMode ism = (ImageStreamMode)ismi.next();
                    ismi.remove();
                    colorStream.setMode(ism);
                    if(colorStream.getMode().getFormat().getCode()== PixelFormat.YUVY.getCode() && colorStream.getMode().getHeight() == 480 && colorStream.getMode().getWidth() == 640){
                        break;
                    }
                }
                colorStream.start();

                BodyStream bodyStream = BodyStream.get(reader);
                bodyStream.start();

                reader.addFrameListener(new StreamReader.FrameListener() {
                    public void onFrameReady(StreamReader reader, ReaderFrame frame) {
                        BodyFrame bodyFrame = BodyFrame.get(frame);

                        Body body = null;
                        if(bodyFrame.getBodies().toString()!="[]"){
                            if(isBodyTracking == 0)  isBodyTracking = 1;
                            body = bodyFrame.getBodies().iterator().next();
                            if(isBodyTracking == 2){
                                bodyData.bodyDataPrint(efc.getBout(), body, cnt);
                            }
                        }

                        //try {
                        //    TimeUnit.MILLISECONDS.sleep(1);
                        //} catch (InterruptedException e) {
                        //    e.printStackTrace();
                        //}
                        ColorFrame colorFrame = ColorFrame.get(frame);
                        Bitmap tempBitmap = rgbData.rgbToArgb(colorFrame.getByteBuffer(), colorFrame.getWidth(), colorFrame.getHeight(), efc.getOut(), isBodyTracking, cnt);
                        rgbData.resetCanvas(camHolder);
                        rgbData.addSkeletonToRGBData(tempBitmap, body,  efc.getOut(), cnt, isBodyTracking);
                        camHolder.unlockCanvasAndPost(rgbData.getRgbCanvas());
                        tempBitmap.recycle();
                    }
                });

                while (!thread_stop) {
                    Astra.update();
                    TimeUnit.MILLISECONDS.sleep(1);
                    if(isBodyTracking == 2){
                        cnt = cnt + 1;
                        recordProgressPercent = (int)(((float)cnt / (float)rgbData.getNUMBEROFFRAME()) * (float)100);
                        recordProgressHandler.sendMessage(recordProgressHandler.obtainMessage());

                        if(cnt==rgbData.getNUMBEROFFRAME()) {
                            isBodyTracking = 0;
                            break;
                        }
                    }
                    else if(isBodyTracking == 1){
                        feedbackStr = "신체 탐색 완료. 1초 뒤 시작";
                        textHandler.sendMessage(textHandler.obtainMessage());
                        TimeUnit.MILLISECONDS.sleep(1000);
                        isBodyTracking = 2;
                        feedbackStr = "녹화 중";
                        textHandler.sendMessage(textHandler.obtainMessage());
                    }
                }

                bodyStream.stop();
                colorStream.stop();
                streamSet.close();

                feedbackStr = "파일 저장 중";
                textHandler.sendMessage(textHandler.obtainMessage());
                rgbData.printRGBData(efc.getRgbout(), efc.getOut());

                feedbackStr = "동영상 인코딩 중";
                textHandler.sendMessage(textHandler.obtainMessage());

                rgbData.makeVideo();

                feedbackStr = "파일 저장 완료";
                textHandler.sendMessage(textHandler.obtainMessage());
            } catch (Throwable e) {
                efc.getOut().println(e);
            } finally {
                aac.terminate();
                try {
                    efc.closeLoggingFile();
                    efc.closeBodyFile_W();
                    efc.closeRGBFile_W();
                } catch (Throwable e){ }
            }
        }
    }
}