package com.example.cheng.mouseinandroidtvbyremotecontrol;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.net.DatagramSocket;

public class MouseAccessibilityService extends AccessibilityService {
    public static final String TAG = MouseAccessibilityService.class.getName();
    public static View cursorView;
    public static LayoutParams cursorLayout;
    public static WindowManager windowManager;
    private DatagramSocket udpSocket;
    private int x;
    private Runnable runnable=new Runnable() {
        @Override
        public void run() {
            onMouseMove(new MouseEvent(x));
        }
    };
    private static void logNodeHierachy(AccessibilityNodeInfo nodeInfo, int depth) {
        Rect bounds = new Rect();
        nodeInfo.getBoundsInScreen(bounds);

        StringBuilder sb = new StringBuilder();
        if (depth > 0) {
            for (int i=0; i<depth; i++) {
                sb.append("  ");
            }
            sb.append("\u2514 ");
        }
        sb.append(nodeInfo.getClassName());
        sb.append(" (" + nodeInfo.getChildCount() +  ")");
        sb.append(" " + bounds.toString());
        if (nodeInfo.getText() != null) {
            sb.append(" - \"" + nodeInfo.getText() + "\"");
        }
        Log.v(TAG, sb.toString());

        for (int i=0; i<nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            if (childNode != null) {
                logNodeHierachy(childNode, depth + 1);
            }
        }
    }


    private static AccessibilityNodeInfo findSmallestNodeAtPoint(AccessibilityNodeInfo sourceNode, int x, int y) {
        Rect bounds = new Rect();
        sourceNode.getBoundsInScreen(bounds);

        if (!bounds.contains(x, y)) {
            return null;
        }

        for (int i=0; i<sourceNode.getChildCount(); i++) {
//            AccessibilityNodeInfo nearestSmaller = findSmallestNodeAtPoint(sourceNode.getChild(i), x, y);
            AccessibilityNodeInfo nearestSmaller = findSmallestNodeAtPoint(sourceNode.getChild(i), x, y);
            if (nearestSmaller != null) {
                return nearestSmaller;
            }
        }
        return sourceNode;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(MouseAccessibilityService.class.getName(),"onCreate");
        cursorView = View.inflate(getBaseContext(), R.layout.cursor, null);
        cursorLayout = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_SYSTEM_ERROR,
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.LEFT;
        cursorLayout.x = 250;
        cursorLayout.y = 160;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

//        try {
//            udpSocket = new DatagramSocket(9999);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    byte[] buffer = new byte[1];
//                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                    while (true) {
//                        try {
//                            udpSocket.receive(packet);
//                            String message = new String(packet.getData()).trim();
//                            final int event = Integer.parseInt(message);
//                            new Handler(getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    onMouseMove(new MouseEvent(event));
//                                }
//                            });
//                        } catch (IOException e) {}
//                    }
//                }
//            }).start();
//        } catch (SocketException e) {
//            throw new RuntimeException(e);
//        }

        registerReceiver(mBroadcast, new IntentFilter(TAG));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(MouseAccessibilityService.class.getName(), "onDestroy");
        unregisterReceiver(mBroadcast);
//        if (windowManager!=null&&cursorView != null) {
////            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeViewImmediate(cursorView);
//            windowManager.removeViewImmediate(cursorView);
//        }
    }

    private void click() {
        Log.d(TAG, String.format("Click [%d, %d]", cursorLayout.x, cursorLayout.y));
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) return;
        AccessibilityNodeInfo nearestNodeToMouse = findSmallestNodeAtPoint(nodeInfo, cursorLayout.x+20, cursorLayout.y+20);
        if (nearestNodeToMouse != null) {
            logNodeHierachy(nearestNodeToMouse, 0);
            nearestNodeToMouse.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
            nearestNodeToMouse.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        nodeInfo.recycle();
    }

    public void onMouseMove(MouseEvent event) {
        switch (event.direction) {
            case MouseEvent.MOVE_LEFT:
                cursorLayout.x -= 20;
                break;
            case MouseEvent.MOVE_RIGHT:
                cursorLayout.x += 20;
                break;
            case MouseEvent.MOVE_UP:
                cursorLayout.y -= 20;
                break;
            case MouseEvent.MOVE_DOWN:
//                performGlobalAction(GESTURE_SWIPE_DOWN);
                cursorLayout.y += 20;
                break;
            case MouseEvent.LEFT_CLICK:
                click();
                break;
            default:
                break;
        }
        windowManager.updateViewLayout(cursorView, cursorLayout);
    }
    private BroadcastReceiver mBroadcast = new BroadcastReceiver() {

        @Override
        public void onReceive(Context mContext, Intent mIntent) {

            if (TAG.equals(mIntent.getAction())) {
                x =mIntent.getIntExtra("move",0);
                Handler handler=new Handler();
                handler.postDelayed(runnable,50);
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        onMouseMove(new MouseEvent(x));
//                    }
//                }).start();
            }
        }
    };
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(MouseAccessibilityService.class.getName(), "onStartCommand");
        windowManager.addView(cursorView, cursorLayout);
        return START_STICKY;
    }
}
