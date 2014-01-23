package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

import java.io.RandomAccessFile;
import java.io.IOException;

public class SymbolIndexMappingPacket extends Packet
{
	private long symbolIndex;
	private String symbol;
	private long marketID;
	private long systemID;
	private char exchangeCode;
	private long priceScaleCode;
	private char securityType;
	private long lotSize;
	private long prevClosePrice;
	private long prevCLoseVolume;
	private long priceResolution;
	private char roundLot;
	
	public SymbolIndexMappingPacket(RandomAccessFile file) throws IOException
	{
		super(file);		
					
		this.symbolIndex = readLong(4);	
		this.symbol = readString(11);		
			
		//Jump the filler
		file.read();		
			
		this.marketID = readLong(2);
		this.systemID = readLong(1);
		
		this.exchangeCode = file.readChar();		
		this.priceScaleCode = readLong(1);
		this.securityType = file.readChar();
		this.lotSize = readLong(2);
		
		this.prevClosePrice = readLong(4);
		this.prevCLoseVolume = readLong(4);
		this.priceResolution = readLong(4);
		this.roundLot = file.readChar();
	}
}