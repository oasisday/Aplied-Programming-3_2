package com.example.drawing;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.graphics.Paint.Join.ROUND;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

//pdf기능수행을 위한 import들
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MyPaintView myView;
    int count = 0;
    int Stroke_Size = 10;
    int Print_Stroke_Size = 1;


    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("필기 노트");
        myView = new MyPaintView(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);
        checkExternalStorage();

        ((LinearLayout) findViewById(R.id.paintLayout)).addView(myView);
        ((RadioGroup) findViewById(R.id.radioGroup)).setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {

                        switch (checkedId) {
                            case R.id.btnRed:
                                myView.mPaint.setColor(Color.RED);
                                break;
                            case R.id.btnErase:
                                myView.mPaint.setColor(Color.WHITE);
                                break;
                            case R.id.btnBlue:
                                myView.mPaint.setColor(Color.BLUE);
                                break;
                            case R.id.btnBlack:
                                myView.mPaint.setColor(Color.BLACK);
                                break;
                        }

                    }
                }
        );

        /*
        Button btnTh = findViewById(R.id.btnTh);
        btnTh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (count % 2 == 1) {
                    btnTh.setText("얇은 선");
                    myView.mPaint.setStrokeWidth(10);
                    count++;
                } else {
                    btnTh.setText("굵은 선");
                    myView.mPaint.setStrokeWidth(20);
                    count++;
                }
            }
        });
        */

        TextView Write_Size = findViewById(R.id.Write_Size);

        Button btnPlus = findViewById(R.id.btnPlus);
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Stroke_Size += 3;
                Print_Stroke_Size += 1;
                myView.mPaint.setStrokeWidth(Stroke_Size);
                Write_Size.setText(String.format("      %d      ",Print_Stroke_Size));
            }
        });

        Button btnMinus = findViewById(R.id.btnMinus);
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Stroke_Size -= 3;
                Print_Stroke_Size -= 1;

                //너무 작아지면 안됨//
                if(Stroke_Size < 10) {
                    Stroke_Size = 10;
                    Print_Stroke_Size = 1;
                }

                myView.mPaint.setStrokeWidth(Stroke_Size);
                Write_Size.setText(String.format("      %d      ",Print_Stroke_Size));
            }
        });


        ((Button) findViewById(R.id.btnClear)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myView.mBitmap.eraseColor(Color.TRANSPARENT);
            }
        });

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 외부 저장소에 쓰기 권한이 있는지 확인
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 권한이 부여되지 않은 경우 권한 요청
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                } else {
                    // 권한이 이미 부여된 경우 비트맵으로 저장
                    saveBitmap();
                    Toast.makeText(getApplicationContext(), "저장성공!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private class MyPaintView extends View {
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint mPaint;

        public MyPaintView(Context context) {

            super(context);
            mPath = new Path();
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(Stroke_Size);
            mPaint.setStrokeJoin(ROUND);
            mPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, null); //지금까지 그려진 내용
            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPath.reset();
                    mPath.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    mPath.lineTo(x, y);
                    mCanvas.drawPath(mPath, mPaint);
                    mPath.reset();
                    break;
            }
            this.invalidate();
            return true;
        }

    }

    private void checkExternalStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Toast.makeText(getApplicationContext(), "외부메모리 읽기 쓰기 모두 가능", Toast.LENGTH_SHORT).show();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Toast.makeText(getApplicationContext(), "외부메모리 읽기만 가능", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "외부메모리 읽기쓰기 모두 안됨 : " + state, Toast.LENGTH_SHORT).show();
        }
    }

    /*
    private void saveBitmapAsPDF() {
        try {
            // PDF를 저장할 파일 경로 정의
            String filePath = Environment.getExternalStorageDirectory() + "/my_note.pdf";

            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(filePath));
            Document document = new Document(pdfDocument);

            // PDF 페이지 크기 (필요한 경우 조정)
            PageSize pageSize = new PageSize(myView.mBitmap.getWidth(), myView.mBitmap.getHeight());
            pdfDocument.setDefaultPageSize(pageSize);

            // PDF 문서에 새 페이지 추가
            PdfPage page = pdfDocument.addNewPage(pageSize);

            // 비트맵을 Image로 변환
            ImageData imageData = ImageDataFactory.create(myView.mBitmap.getNinePatchChunk());
            Image image = new Image(imageData);

            // 페이지 내 이미지 위치 및 스케일 설정
            image.setAutoScale(true);
            image.setFixedPosition(0, 0);

            // 이미지를 PDF 페이지에 추가
            document.add(image);

            // 문서를 닫고 저장
            document.close();
            pdfDocument.close();

            // PDF 저장 성공 메시지 표시
            Toast.makeText(getApplicationContext(), "PDF 저장됨: " + filePath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "PDF 저장 오류", Toast.LENGTH_SHORT).show();
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여된 경우 PDF로 저장
                saveBitmapAsPDF();
            } else {
                // 권한이 거부된 경우 사용자에게 메시지 표시
                Toast.makeText(this, "권한이 거부되었습니다. PDF로 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    */

    private void saveBitmap() {
        try {
            // 외부 저장소에 저장할 파일 경로 정의
            File file = new File(Environment.getExternalStorageDirectory(), "my_bitmap.png");

            // 파일 경로에 해당 비트맵 저장
            FileOutputStream fos = new FileOutputStream(file);
            myView.mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // 저장 성공 메시지 표시
            Toast.makeText(getApplicationContext(), "비트맵 저장됨: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            // 저장 오류 메시지 표시
            Toast.makeText(getApplicationContext(), "비트맵 저장 오류", Toast.LENGTH_SHORT).show();
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여된 경우 비트맵을 저장
                saveBitmap();
            } else {
                // 권한이 거부된 경우 사용자에게 메시지 표시
                Toast.makeText(this, "권한이 거부되었습니다. 비트맵을 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}