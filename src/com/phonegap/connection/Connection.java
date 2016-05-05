package com.phonegap.connection;

import java.util.ArrayList;

import org.apache.cordova.DroidGap;
import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import com.yesorno.MainActivity;
import com.yesorno.PotencialAgente;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.webkit.WebView;
import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.MicroRuntime;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;
import chat.client.agent.ChatClientAgent;
import chat.client.agent.ChatClientInterface;
import chat.ontology.ChatOntology;
import chat.ontology.Joined;
import chat.ontology.Left;
import android.content.Intent;
import android.content.Context;

public class Connection extends DroidGap 
{
	
	public WebView mAppView;
	public DroidGap mGap;
	public String nickname;
	public MainActivity main;
	
	public Connection(WebView view, DroidGap gap, String Nickname, MainActivity Main)
	{
		mAppView = view;
		nickname = Nickname;
		mGap = gap;
		main = Main;
 	}
	
	public String getTelephoneNumber()
	{
	    //TelephonyManager tm = (TelephonyManager) mGap.getSystemService(Context.TELEPHONY_SERVICE);
	    //String number = tm.getLine1Number();
	    return "RETORNEI UM VALOR";
	}
	
	public String retornaNome()
	{
		
		try 
		{
			ChatClientInterface chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
			return chatClientInterface.retornaNome();
		} 
		catch (StaleProxyException e) {return "StaleProxyException";} 
		catch (ControllerException e) {return "ControllerException";}
	}
	
	public void setPosition(String latitude, String longitude)
	{
		try 
		{
			ChatClientInterface chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
			chatClientInterface.setPosition(latitude,longitude, main);
		} 
		catch (StaleProxyException e) {} 
		catch (ControllerException e) {}
	}

	public String obtemAgentes()
	{
		return "vazio";
	}
		
	public String removePalavroes(String palavroes[], String frase)
	{
		for(int i = 0; i < palavroes.length; i++)
		{
			frase.replaceAll(palavroes[i], frase);
		}
		return frase;
	}
		
	public static String removerAcentos(String str) 
	{
		return str.replaceAll("б|а|в|г|д","a")  
		        .replaceAll("й|и|к|л","e")
				.replaceAll("н|м|о|п","i")
				.replaceAll("у|т|ф|х|ц","o") 
				.replaceAll("ъ|щ|ы|ь","u")
				.replaceAll("з","c"); 		
	}
	
	public void localizaAgentePotencial(String frase)
	{
		String localizacao = geraPosicao(frase);
		
		String posicoes[] = localizacao.split(";");
		double latitude = Double.parseDouble(posicoes[0]);
		double longitude = Double.parseDouble(posicoes[1]);
		
		//System.out.println("A localizaзгo й: " + localizacao);
		PotencialAgente pot = null;
		
		try 
		{
			ChatClientInterface chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
			pot = chatClientInterface.verificaPotenciaisAgentes(latitude,longitude,frase);
		} 
		catch (StaleProxyException e) {} 
		catch (ControllerException e) {}

	}

	public void enviaResposta(String data)
	{

		String dados[] = data.split(";");
		
		String agente = dados[0];
		String resposta = dados[1];
		
		//System.out.println("Recebi uma resposta de " + agente + " - Com a resposta: " + resposta);
		
		try 
		{
			ChatClientInterface chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
			chatClientInterface.enviaResposta(agente,resposta);
		} 
		catch (StaleProxyException e) {} 
		catch (ControllerException e) {}
		
	}
	
	public String geraPosicao(String pergunta)
	{
		String erro = "";
		
		pergunta = pergunta.toLowerCase();
		pergunta = removerAcentos(pergunta);
		
		String palavrasLocalidade[] = {"ru","restaurante","bloco","estacionamento","carrefour","atletica","dce","axis",
									   "onibus","secretaria","facul","faculdade","ufabc","teste"};
		
		String palavroes[] = {"filho da puta", "caralho", "cacete", "buceta", "xoxota", "merda"};
		
		pergunta = removePalavroes(palavroes,pergunta);
		
		String palavras[] = pergunta.split(" ");
		
		String localizacao = "0;0";
		
		for(int i = 0; i < palavras.length; i++)
		{
			for(int j = 0; j < palavrasLocalidade.length; j++)
			{
				if(palavras[i].equals(palavrasLocalidade[j]))
				{
					if(palavras[i].equals("bloco"))
					{
						int proximaPosicao = i + 1;
						if(proximaPosicao <= palavras.length)
						{
							if(palavras[i+1] == "a")
							{
								localizacao = "-23.644885;-46.527990";
							}
							else if(palavras[i+1] == "b")
							{
								localizacao = "-23.645873;-46.527665";
							}
							else
							{
								erro += "Qual Bloco?\n";
							}
						}
					}
					else if(palavras[i].equals("restaurante"))
					{
						int proximaPosicao = i + 1;
						if(proximaPosicao <= palavras.length)
						{
							if(palavras[i+1] == "universitario")
							{
								localizacao = "-23.644855;-46.526817";
							}
							else
							{
								erro += "Este restaurante nao esta cadastrado?\n";
							}
						}
					}
					else if(palavras[i].equals("ru"))
					{
						localizacao = "-23.644855;-46.526817";
					}
					else if(palavras[i].equals("estacionamento"))
					{
						localizacao = "-23.644059;-46.527056";
					}
					else if(palavras[i].equals("carrefour"))
					{
						localizacao = "-23.647285;-46.526037";
					}
					else if(palavras[i].equals("atletica"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("dce"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("axis"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("onibus"))
					{
						localizacao = "-23.643706;-46.527096";
					}
					else if(palavras[i].equals("secretaria"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("facul"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("faculdade"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("ufabc"))
					{
						localizacao = "-23.644885;-46.527990";
					}
					else if(palavras[i].equals("teste"))
					{
						localizacao = "-23.536084;-46.807991";
					}
				}
			}
		}
		
		return localizacao;
	}
}
