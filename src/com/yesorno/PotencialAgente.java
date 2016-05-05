package com.yesorno;

public class PotencialAgente implements java.io.Serializable
{
	int tipo;
	String nome;
	String frase;
	
	public PotencialAgente(int Tipo, String Nome, String Frase)
	{
		this.tipo = Tipo;
		this.nome = Nome;
		this.frase = Frase;
	}
	
	public String retornaNome()
	{
		return nome;
	}
	
	public String retornaFrase()
	{
		return frase;
	}
	
	public int retornaTipo()
	{
		return tipo;
	}
	
}
