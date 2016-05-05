package chat.client.agent;

import android.webkit.WebView;

import com.yesorno.MainActivity;
import com.yesorno.PotencialAgente;

/**
 * This interface implements the logic of the chat client running on the user
 * terminal.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public interface ChatClientInterface 
{
	public void handleSpoken(String s);
	public String[] retornaTodosAgentes();
	public String retornaNome();
	
	//POSICIONAMENTO GPS
	public void setPosition(String latitude, String longitude, MainActivity main);
	public double retornaLatitude();
	public double retornaLongitude();
	
	public PotencialAgente verificaPotenciaisAgentes(double latitude, double longitude, String frase);
	
	public void enviaResposta(String agente, String resposta);
}