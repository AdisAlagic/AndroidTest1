package com.adisalagic.hashfinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.adisalagic.IFindHash;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private       IFindHash findHash;
    private final String    PATH = "com.adisalagic.hashfindedserver.IFindHash";

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            findHash = IFindHash.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            findHash = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(PATH);
        intent = createExplicitIntent(this, intent);
        if (intent != null){
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSubmitClick(View view){
        if (findHash == null){
            Toast.makeText(this, R.string.service_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        EditText hash = findViewById(R.id.eHash);
        if (!verifyHash(hash.getText().toString())){
            Toast.makeText(this, R.string.invalid_hash, Toast.LENGTH_SHORT).show();
            return;
        }
        String hashedString = hash.getText().toString();
        boolean ok = true;
        try{
            ok = findHash.findHash(hashedString);
        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.remote_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
        TextView textView = findViewById(R.id.result);
        if (ok){
            textView.setTextColor(Color.GREEN);
            textView.setText(R.string.hash_found);
        }else {
            textView.setTextColor(Color.BLACK);
            textView.setText(R.string.hash_not_found);
        }
    }

    /**
     * Метод ищет конкретный service.
     * @param context {@link Context}
     * @param intent {@link Intent}
     * @return Нужный {@link Intent}
     */
    public Intent createExplicitIntent(Context context, Intent intent){
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent, 0);

        if (resolveInfos.size() == 0){
            return null;
        }

        ResolveInfo serviceInfo = resolveInfos.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        Intent explicitIntent = new Intent(intent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    public static boolean verifyHash(String hash){
        Pattern pattern = Pattern.compile("[a-f0-9]{32}");
        Matcher matcher = pattern.matcher(hash);
        return matcher.find();
    }
}