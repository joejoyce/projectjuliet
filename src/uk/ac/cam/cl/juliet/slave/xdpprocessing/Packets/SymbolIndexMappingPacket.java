package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public class SymbolIndexMappingPacket extends Packet {
	private long symbolIndex;
	private String symbol;
	private long marketID;
	private long systemID;
	private char exchangeCode;
	private long priceScaleCode;
	private char securityType;
	private long lotSize;
	private long prevClosePrice;
	private long prevCloseVolume;
	private long priceResolution;
	private char roundLot;

	public SymbolIndexMappingPacket(int[] data) {
		super(data);

		this.symbolIndex = readLong(4);
		this.symbol = readString(11);

		// Jump the filler
		readChar();

		this.marketID = readLong(2);
		this.systemID = readLong(1);

		this.exchangeCode = readChar();
		this.priceScaleCode = readLong(1);
		this.securityType = readChar();
		this.lotSize = readLong(2);

		this.prevClosePrice = readLong(4);
		this.prevCloseVolume = readLong(4);
		this.priceResolution = readLong(1);
		this.roundLot = readChar();
	}

	public String toString() {
		return "index: " + symbolIndex + ", symbol: " + symbol;
	}
}