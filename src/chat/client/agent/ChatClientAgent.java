/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package chat.client.agent;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

import com.yesorno.MainActivity;
import com.yesorno.PotencialAgente;

import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import chat.ontology.ChatOntology;
import chat.ontology.Joined;
import chat.ontology.Left;
import android.content.Intent;
import android.content.Context;
import android.webkit.WebView;

public class ChatClientAgent extends Agent implements ChatClientInterface 
{
	private static final long serialVersionUID = 1594371294421614291L;

	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private static final String CHAT_ID = "__chat__";
	private static final String CHAT_MANAGER_NAME = "manager";

	private Set participants = new SortedSetImpl();
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private ACLMessage spokenMsg;

	private Context context;
	AMSAgentDescription[] agentes = null;
	
	private double latitude;
	private double longitude;
	
	private boolean flag = false;
	
	private MainActivity main;
	
	public String nickname;
	
	protected void setup() 
	{
		
		//IMPORTANTE!!!---------------------------------------------
		registerO2AInterface(ChatClientInterface.class, this);
		nickname = this.getLocalName();
		//----------------------------------------------------------
		
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) 
		{
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
		}
		
		addBehaviour(new CyclicBehaviour(this)
		{
			public void action()
			{
				ACLMessage msg = receive();
				
				if(msg != null)
				{
					System.out.println("Mensagem " + msg.getContent());
					
					try 
					{
						PotencialAgente content = (PotencialAgente) msg.getContentObject();			
						String dados;
						
						switch(content.retornaTipo())
						{
							case 0:
								
								dados = content.retornaNome() + ";" + content.retornaFrase();
								
								System.out.println("Recebi uma Mensagem com uma pergunta!");
								System.out.println("Remetente: " + content.retornaNome());
								System.out.println("Destinatario: Eu");
								
								main.recebePergunta(dados);

								break;
							
							case 1:
								
								String agente = content.retornaNome();
								int resposta = Integer.parseInt(content.retornaFrase());
								
								System.out.println("Recebi uma Mensagem com um resposta!");
								System.out.println("Remetente: " + agente);
								System.out.println("Destinatario: Eu");
								
								main.recebeResposta(agente,resposta);
								main.enviaSMS(nickname, "Você recebeu uma nova resposta!", "1");
								
								break;
						}
						
					}
					catch (Exception ex) 
					{ 
						System.out.println("Erro ao desserializar!");
						System.out.println(ex);
					}
				}
				
			};
		});
	}
	

	protected void takeDown() 
	{
		try
		{
			DFService.deregister(this);
		}
		catch(FIPAException e)
		{
			System.out.println(e);
		}
	}
	

	private void notifyParticipantsChanged() 
	{
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
		context.sendBroadcast(broadcast);
	}

	private void notifySpoken(String speaker, String sentence) 
	{
		Intent broadcast = new Intent();
		broadcast.setAction("jade.demo.chat.REFRESH_CHAT");
		broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
		logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
		context.sendBroadcast(broadcast);
	}

	class ParticipantsManager extends CyclicBehaviour 
	{
		private static final long serialVersionUID = -4845730529175649756L;
		private MessageTemplate template;

		ParticipantsManager(Agent a) {
			super(a);
		}

		public void onStart() 
		{
			ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
			subscription.setLanguage(codec.getName());
			subscription.setOntology(onto.getName());
			String convId = "C-" + myAgent.getLocalName();
			subscription.setConversationId(convId);
			subscription.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME));
			myAgent.send(subscription);
			template = MessageTemplate.MatchConversationId(convId);
		}

		public void action() 
		{
			// Receives information about people joining and leaving
			// the chat from the ChatManager agent
			ACLMessage msg = myAgent.receive();
			
			if (msg != null) 
			{
				System.out.println("Recebi uma mensagem: " + msg.getContent());
			} 
			else 
			{
				block();
			}
		}
	} // END of inner class ParticipantsManager

	/**
	 * Inner class ChatListener. This behaviour registers as a chat participant
	 * and keeps the list of participants up to date by managing the information
	 * received from the ChatManager agent.
	 */
	class ChatListener extends CyclicBehaviour 
	{
		private static final long serialVersionUID = 741233963737842521L;
		private MessageTemplate template = MessageTemplate
				.MatchConversationId(CHAT_ID);

		ChatListener(Agent a) 
		{
			super(a);
		}

		public void action() 
		{
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) 
			{
				if (msg.getPerformative() == ACLMessage.INFORM) 
				{
					notifySpoken(msg.getSender().getLocalName(),
							msg.getContent());
				} 
				else 
				{
					handleUnexpected(msg);
				}
			} 
			else 
			{
				block();
			}
		}
	} // END of inner class ChatListener

	/**
	 * Inner class ChatSpeaker. INFORMs other participants about a spoken
	 * sentence
	 */
	private class ChatSpeaker extends OneShotBehaviour 
	{
		private static final long serialVersionUID = -1426033904935339194L;
		private String sentence;

		private ChatSpeaker(Agent a, String s) 
		{
			super(a);
			sentence = s;
		}

		public void action() 
		{
			spokenMsg.clearAllReceiver();
			Iterator it = participants.iterator();
			
			while (it.hasNext()) 
			{
				spokenMsg.addReceiver((AID) it.next());
			}
			
			spokenMsg.setContent(sentence);
			notifySpoken(myAgent.getLocalName(), sentence);
			send(spokenMsg);
		}
	} // END of inner class ChatSpeaker

	// ///////////////////////////////////////
	// Methods called by the interface
	// ///////////////////////////////////////
	public void handleSpoken(String s) 
	{
		// Add a ChatSpeaker behaviour that INFORMs all participants about
		// the spoken sentence
		addBehaviour(new ChatSpeaker(this, s));
	}
	
	
	//NOVOS MÉTODOS JADE--------------------------------------------------------------------
	public String retornaNome() 
	{
		return this.getLocalName();
	}
	
	public void enviaResposta(String agente, String resposta)
	{
		System.out.println("Enviando resposta para o agente: " + agente);
		
		ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
		msg.setOntology("Location-ontology");
		msg.setLanguage("Portuguese");
		msg.addReceiver(new AID(agente, AID.ISLOCALNAME));
		
		PotencialAgente pot = new PotencialAgente(1,this.retornaNome(),resposta);
		
		try 
		{
			msg.setContentObject(pot);
		}
		catch (Exception ex) 
		{ 
			System.out.println("Erro ao serializar!");
			System.out.println(ex);
		}
		
		send(msg);
		System.out.println("Resposta enviada com Sucesso!");
	}
	
	public PotencialAgente verificaPotenciaisAgentes(double latitude, double longitude, String frase)
	{
		DFAgentDescription template = new DFAgentDescription();
		
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Localizacao");
		
		String out = "0";
		PotencialAgente pot = null;
		
		int qtdAgentes = 0;
		
		String nomeAgentes = "";
		
		try
		{
			DFAgentDescription[] result = DFService.search(this, template);

			for(int i = 0; i < result.length; i++)
			{
				//out = result[i].getName().getLocalName() + " prove\n";
				if(!result[i].getName().getLocalName().equals(this.retornaNome()))
				{
					Iterator iter = result[i].getAllServices();
					
					while(iter.hasNext())
					{
						ServiceDescription SD = (ServiceDescription) iter.next();
						
						String posicoes[] = SD.getName().split(";");
						
						float latitudeAgente = Float.parseFloat(posicoes[0]);
						float longitudeAgente = Float.parseFloat(posicoes[1]);
						
						double distancia = distance((float)latitude,(float)longitude,latitudeAgente,longitudeAgente);
						
						if(distancia < 2000)
						{
							ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
							msg.setOntology("Location-ontology");
							msg.setLanguage("Portuguese");
							msg.addReceiver(new AID(result[i].getName().getLocalName(), AID.ISLOCALNAME));
							
							main.enviaSMS(result[i].getName().getLocalName(), "Você recebeu uma nova pergunta!", "1");
							
							pot = new PotencialAgente(0,this.retornaNome(),frase);
							
							nomeAgentes += result[i].getName().getLocalName() + ";";
							qtdAgentes++;
							
							try 
							{
								msg.setContentObject(pot);
							}
							catch (Exception ex) 
							{ 
								System.out.println("Erro ao serializar!");
								System.out.println(ex);
							}

							send(msg);
							System.out.println("Mensagem enviada com Sucesso!");
						}
					}
				}
			}
		}
		catch(FIPAException e)
		{
			System.out.println(e);
		}
		
		if(qtdAgentes > 0)
		{
			main.successAgent(frase);
		}
		else
		{
			main.errorAgent();
		}
				
		return pot;
	}
	
	public static float distance(float lat1, float lng1, float lat2, float lng2) 
	{
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    int meterConversion = 1609;

	    return new Float(dist * meterConversion).floatValue();
    }
	
	public void setPosition(String Latitude, String Longitude, MainActivity Main)
	{
		this.main = Main;
		
		if(!flag)
		{
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Localizacao");
			sd.setName(Latitude + ";" + Longitude);
			dfd.addServices(sd);
			
			try
			{
				DFService.register(this, dfd);
			}
			catch(FIPAException e)
			{
				System.out.println(e);
			}
			flag = true;
		}
		else
		{
			
		}
	}
	
	public double retornaLatitude()
	{
		return latitude;
	}
	
	public double retornaLongitude()
	{
		return longitude;
	}
	//--------------------------------------------------------------------------------------
	
	
	
	public String[] retornaTodosAgentes() 
	{
		String[] pp = {"Oi"};

		
		return pp;
	}

	// ///////////////////////////////////////
	// Private utility method
	// ///////////////////////////////////////
	private void handleUnexpected(ACLMessage msg) 
	{
		if (logger.isLoggable(Logger.WARNING)) {
			logger.log(Logger.WARNING, "Unexpected message received from "
					+ msg.getSender().getName());
			logger.log(Logger.WARNING, "Content is: " + msg.getContent());
		}
	}

}
