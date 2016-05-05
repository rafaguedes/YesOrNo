package com.yesorno;

import static com.yesorno.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.yesorno.CommonUtilities.EXTRA_MESSAGE;
import static com.yesorno.CommonUtilities.SENDER_ID;

import java.util.logging.Level;

import jade.android.AndroidHelper;
import jade.android.MicroRuntimeService;
import jade.android.MicroRuntimeServiceBinder;
import jade.android.RuntimeCallback;
import jade.core.Agent;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.Display;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import chat.client.agent.ChatClientAgent;
import chat.client.agent.ChatClientInterface;

import com.google.android.gcm.GCMRegistrar;
import com.phonegap.connection.Connection;


public class MainActivity extends DroidGap 
{
	
	protected float ORIG_APP_W = 480;
    protected float ORIG_APP_H = 854;
    
    //DETECTOR DE CONEXAO
    ConnectionDetector cd;
    AlertDialogManager alert = new AlertDialogManager();
    AsyncTask<Void, Void, Void> mRegisterTask;
    TextView lblMessage;
    
    boolean inicio = true;
    
    final Properties profile = new Properties();
    private MicroRuntimeServiceBinder microRuntimeServiceBinder;
	private ServiceConnection serviceConnection;
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	private MyHandler myHandler;
	
	private Connection interfaceJavaScript;
	Agent agente;
	
	String nickName;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
                
        cd = new ConnectionDetector(getApplicationContext());

        //VERIFICA SE ESTÁ CONECTADO
        if (!cd.isConnectingToInternet()) 
        {
            alert.showAlertDialog(MainActivity.this, "Problema com a conexão de Internet","Por favor conecte-se a Internet para utilizar este aplicativo.", false);
            return;
        }
        
        //VERIFICAÇÕES DO GCM //////////////////////////////////////////////////////////////////////////////////
        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
        
        registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));
        
        //Toast.makeText(getApplicationContext(), "Verificando...", Toast.LENGTH_LONG).show();
        
        final String regId = GCMRegistrar.getRegistrationId(this);
        
        if (regId.equals("")) 
        {
            GCMRegistrar.register(this, SENDER_ID);
            inicio = true;
        }
        else
        {
        	inicio = false;
        	
        	if (GCMRegistrar.isRegisteredOnServer(this)) 
        	{
                //Toast.makeText(getApplicationContext(), "Ok...", Toast.LENGTH_LONG).show();
            }
        	else
        	{
                final Context context = this;
                
                //Toast.makeText(getApplicationContext(), "Gerando ID...", Toast.LENGTH_LONG).show();
                
                mRegisterTask = new AsyncTask<Void, Void, Void>() 
                {
                    protected Void doInBackground(Void... params) 
                    {
                        // Register on our server
                        // On server creates a new user
                        //ServerUtilities.register(context, "", "", regId);
                        return null;
                    }
 
                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }
 
                };
                mRegisterTask.execute(null, null, null);
        	}
        }
        
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        
        //String regId = "APA91bEeQHpPul0hNE_VKCjNB1p15f1yjTYMIRTcIyn25w8hs1DfmLvV-FESacPseCOm4_fw1Jn7PxJ5AUx7wCj5juuL-KOsNPpRzXvwYYbjlb6XQgIEEmwlvGKbWzXsivMf4uL4y2tS";
        
        //==================================================================================================
        //JADE
        //==================================================================================================
        
        //profile.setProperty(Profile.MAIN_HOST, "192.168.0.132");
        profile.setProperty(Profile.MAIN_HOST, "172.31.210.227");
		profile.setProperty(Profile.MAIN_PORT, "1099");
		//profile.setProperty(Profile.LOCAL_HOST, "127.0.0.1");
		//profile.setProperty(Profile.LOCAL_PORT, "1099");
		//profile.setProperty(Profile.);
		profile.setProperty(Profile.MAIN, Boolean.FALSE.toString());
		profile.setProperty(Profile.JVM, Profile.ANDROID);
		
		nickName = regId;
				
		if (AndroidHelper.isEmulator()) 
		{
			profile.setProperty(Profile.LOCAL_HOST, AndroidHelper.LOOPBACK);
		} 
		else 
		{
			profile.setProperty(Profile.LOCAL_HOST,	AndroidHelper.getLocalIPAddress());
		}
		
		profile.setProperty(Profile.LOCAL_PORT, "1099");
		
		if (microRuntimeServiceBinder == null) 
		{	
			serviceConnection = new ServiceConnection() 
			{
				public void onServiceConnected(ComponentName className, IBinder service) 
				{
					microRuntimeServiceBinder = (MicroRuntimeServiceBinder) service;
					System.out.println("Gateway successfully bound to MicroRuntimeService");
					startContainer(nickName, profile, agentStartupCallback);					
				};

				public void onServiceDisconnected(ComponentName className) 
				{
					microRuntimeServiceBinder = null;
					System.out.println("Gateway unbound from MicroRuntimeService");
				}
			};
			
			System.out.println("Binding Gateway to MicroRuntimeService...");
			bindService(new Intent(getApplicationContext(), MicroRuntimeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		}
		else 
		{
			System.out.println("MicroRuntimeGateway already binded to service");
			startContainer(nickName, profile, agentStartupCallback);
		}
		
        //==================================================================================================
        
		//super.loadUrl("file:///android_asset/www/index.html");
		
		super.loadUrl("file:///android_asset/www/principal.html?sid=" + regId);
		
        /*if(inicio)
        {
        	super.loadUrl("file:///android_asset/www/index.html?sid=" + regId);
        }
        else
        {
        	super.loadUrl("file:///android_asset/www/principal.html?sid=" + regId);
        }*/
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth(); 
        int height = display.getHeight(); 
        
        // calculate target scale (only dealing with portrait orientation)
        double globalScale = Math.ceil(( width / ORIG_APP_W ) * 100);

        // set the scale
        this.appView.setInitialScale( (int)globalScale );
        
        //INTERFACE DE CONEXÃO ENTRE O JAVASCRIPT E O JAVA-----------------------------
        interfaceJavaScript = new Connection(this.appView, this, nickName, this);
        appView.addJavascriptInterface(interfaceJavaScript, "InterfaceJavaScript");
        //-----------------------------------------------------------------------------
        
    }
    
    /*public void teste()
    {
    	
    	String valor = "Teste";
    	
    	String js = String.format("window.imprimeTeste('" + valor + "')");
        this.sendJavascript(js);
    }*/
    
    //MÉTODOS DE ACESSO AO CÓDIGO JAVASCRIPT=================================
    public void successAgent(String dados)
    {    	
    	String js = String.format("window.successAgent('%s')",dados);
        this.sendJavascript(js);
    }
    
    public void errorAgent()
    {    	
    	String js = String.format("window.errorAgent()");
        this.sendJavascript(js);
    }
    
    public void mostraPergunta()
    {    	
    	String js = String.format("window.errorAgent()");
        this.sendJavascript(js);
    }
    
    public void recebePergunta(String dados)
    {    	
    	String js = String.format("window.recebePergunta('%s')",dados);
        this.sendJavascript(js);
    }
    
    public void recebeResposta(String agente, int resposta)
    {    	
    	String temp = String.valueOf(resposta);
    	
    	String js = String.format("window.recebeResposta('%s')",temp);
        this.sendJavascript(js);
    }
    
    public void enviaResposta()
    {
    	
    }
    
    public void enviaSMS(String sid, String msg, String tipo)
    {
    	String js = String.format("window.enviaSMS('" + sid + "','" + msg + "','"  + tipo + "')");
        this.sendJavascript(js);
    }
    //=======================================================================
    
      
    private void startContainer(final String nickname, Properties profile, final RuntimeCallback<AgentController> agentStartupCallback) 
    {
    	System.out.println("Iniciando Container");
    	
		if (!MicroRuntime.isRunning()) 
		{
			
			System.out.println("MicroRuntime Running...");

			microRuntimeServiceBinder.startAgentContainer(profile,new RuntimeCallback<Void>() 
			{
				@Override
				public void onSuccess(Void thisIsNull) 
				{
					System.out.println("Successfully start of the container...");
					startAgent(nickname, agentStartupCallback);
				}

				@Override
				public void onFailure(Throwable throwable) 
				{
					System.out.println("Failed to start the container...");
				}
			});
		} 
		else 
		{
			startAgent(nickname, agentStartupCallback);
		}
	}
    
    private void startAgent(final String nickname, final RuntimeCallback<AgentController> agentStartupCallback) 
    {
    	System.out.println("Iniciando Agente");
    	
    	
		microRuntimeServiceBinder.startAgent(nickname, ChatClientAgent.class.getName(), new Object[] { getApplicationContext() }, new RuntimeCallback<Void>() 
		{
			@Override
			public void onSuccess(Void thisIsNull) 
			{
				System.out.println("Successfully start of the "	+ ChatClientAgent.class.getName() + "...");
				try 
				{
					agentStartupCallback.onSuccess(MicroRuntime.getAgent(nickname));	
				} 
				catch (ControllerException e) 
				{
					// Should never happen
					agentStartupCallback.onFailure(e);
				}
			}

			@Override
			public void onFailure(Throwable throwable) 
			{
				System.out.println("Failed to start the " + ChatClientAgent.class.getName() + "...");
				agentStartupCallback.onFailure(throwable);
			}
		});
	}
    
    private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() 
	{
		@Override
		public void onSuccess(AgentController agent) 
		{
			
		}

		@Override
		public void onFailure(Throwable throwable) 
		{
			//System.out.println("Nickname already in use!");
			System.out.println(throwable.getMessage());
		}
	};
	
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            // Waking up mobile if it is sleeping
            WakeLocker.acquire(getApplicationContext());
 
            /**
             * Take appropriate action on this message
             * depending upon your app requirement
             * For now i am just displaying it on the screen
             * */
 
            // Showing received message
            lblMessage.append(newMessage + "\n");
            //Toast.makeText(getApplicationContext(), "New Message: " + newMessage, Toast.LENGTH_LONG).show();
 
            // Releasing wake lock
            WakeLocker.release();
            
            String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);

        }
    };
    
    private class MyHandler extends Handler 
    {
		@Override
		public void handleMessage(Message msg) 
		{
			Bundle bundle = msg.getData();
			if (bundle.containsKey("error")) 
			{
				//infoTextView.setText("");
				String message = bundle.getString("error");
				System.out.println(message);
			}
		}

		public void postError(String error) 
		{
			Message msg = obtainMessage();
			Bundle b = new Bundle();
			b.putString("error", error);
			msg.setData(b);
			sendMessage(msg);
		}
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_yes_or_no, menu);
        return true;
    }
}
