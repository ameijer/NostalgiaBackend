package com.nostalgia.contentserver;

public interface ShellCallback
{
	public void shellOut (String shellLine);
	
	public void processComplete (int exitValue);
}