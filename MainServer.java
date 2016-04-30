
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainServer {

	static LinkedList<PeerRecord> activePeers = new LinkedList<PeerRecord>() ; // [uploaderHostname,uploaderPort ] 
	static LinkedList<RFC> rfcList = new LinkedList<RFC>()	;		// [ rfcNo, rfcTitle, uploaderHostName] 
	static int activePeerCount = 0 ;
	public static final String VERSION = "P2P-CI/1.0" ;
	public static final String OS = System.getProperty("os.name") ;
	public static final String TAB = " " ; // Using \t instead of space because RFC title may contains spaces.
	public static final String LINE_SEPARATOR = "\r\n" ;
	public static Date currentDateTime = new Date() ;

	Socket clientSocket;

	public static void main(String[] args) throws IOException {

		// Well defined port
		final int port = 7734 ;
		ServerSocket serverSocket ;
		try{
			serverSocket = new ServerSocket( port );
			System.out.println("Server " + serverSocket.getInetAddress().getHostName() + " is listening on port " + serverSocket.getLocalPort());

			while (true) {
				Socket clientSocket = serverSocket.accept() ;
				System.out.println("Connection Request Accepted.");
				// Put each new client request in a new thread.
				MainServerChildThread childThread = new MainServerChildThread(clientSocket, rfcList, activePeers) ;
				childThread.start() ;
			}
		}catch( IOException e){
			e.printStackTrace() ;
		}

	} 
}

class MainServerChildThread extends Thread {

	public static final String VERSION = "P2P-CI/1.0" ;
	public static final String OS = System.getProperty("os.name") ;
	public static final String SPACE = " " ; // Using \t instead of space because RFC title may contains spaces.
	public static final String LINE_SEPARATOR = "\r\n" ;
	public static Date currentDateTime = new Date() ;

	LinkedList<RFC> rfcList ;
	LinkedList<PeerRecord> activePeers ;

	Socket clientSocket;

	public MainServerChildThread(Socket clientSocket, LinkedList<RFC> rfcList, LinkedList<PeerRecord> activePeers){
		this.clientSocket = clientSocket ;
		this.activePeers = activePeers ;
		this.rfcList = rfcList ;
	}	


	public void run() {

		OutputStream out  = null ;
		DataOutputStream dos = null ;
		InputStream in  = null;
		DataInputStream dis  = null ;

		try {
			dis = new DataInputStream(clientSocket.getInputStream());
			dos = new DataOutputStream(clientSocket.getOutputStream()) ;

			while( true){
				// Parse the incoming request to determine the request type
				// Accordingly call one of the methods.
				String incomingRequest;
				try {
					incomingRequest = dis.readUTF();
					System.out.println("\nInside MAIN SERVER, the incoming request is : \n\n" + incomingRequest );
					String[] words ;
					// Reading the request line by line.
					String[] lines = incomingRequest.split(LINE_SEPARATOR) ;
					String firstLine = lines[0] ;
					words = firstLine.split(SPACE) ;
					String requestType = words[0] ;
					String response = "" ;
					StatusCode statusCode = null ;
					String version = "" ;
					String uploaderHostName = ""  ;
					int uploaderPort = 0 ;
					int rfcNo = 0 ;

					if("EXIT".equals(requestType )){
						String secondLine = lines[1] ;
						words = secondLine.split(SPACE) ;
						uploaderHostName = words[1] ;

						String thirdLine = lines[2] ;
						words = thirdLine.split(SPACE) ;
						uploaderPort = Integer.parseInt(words[1]) ;
						version = VERSION ;
						response =  exit(uploaderHostName,uploaderPort) ;
					}
					else if( requestType.equals("ADD") || requestType.equals("LOOKUP") ){

						rfcNo = Integer.parseInt(words[2]) ;
						version = words[3] ;

						String secondLine = lines[1] ;
						words = secondLine.split(SPACE) ;
						uploaderHostName = words[1] ;

						String thirdLine = lines[2] ;
						words = thirdLine.split(SPACE) ;
						uploaderPort = Integer.parseInt(words[1]) ;
						String rfcTitle = "" ;
						String fourthLine = lines[3] ;
						words = fourthLine.split(":") ;
						rfcTitle = words[1] ;

						if( "ADD".equals(requestType )){
							response = add(rfcNo, rfcTitle, uploaderHostName, uploaderPort) ;
						}else {// if("LOOKUP".equals(requestType )){
							response = lookup(rfcNo) ;
						}
					}else if( requestType.equals("LIST_ALL")){
						version = words[1] ;

						String secondLine = lines[1] ;
						words = secondLine.split(SPACE) ;
						uploaderHostName = words[1] ;

						String thirdLine = lines[2] ;
						words = thirdLine.split(SPACE) ;
						uploaderPort = Integer.parseInt(words[1]) ;
						response = list() ;
					}else{
						statusCode = StatusCode.BAD_REQUEST ;
						response =  frameResponseForBadReq(version) ;
					}

					if( !VERSION.equals(version)){
						statusCode = StatusCode.P2P_CI_VERSION_NOT_SUPPORTED ;
						response =  frameResponseForInValidVersion(version) ;
					}

					dos.writeUTF(response) ;
					System.out.println("Inside Main Server, the response received is\n\n" +  response );
				}catch (SocketTimeoutException s) {
					System.out.println("Socket Time Out!");
					break;
				} catch (EOFException eof) {
					System.out.println("End Of File Reached!");
					break;
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} 
			}
		}catch (IOException e) {
			e.printStackTrace();
		}

	}


	public String exit(String uploaderServer, int uploaderPort){
		// Delete the list of active peers and RFCs
		removePeer(uploaderServer, uploaderPort) ;
		String response = frameResponseForExit() ;
		return response ;
	}

	private String frameResponseForExit() {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append("Server Exited"  + LINE_SEPARATOR) ;
		responseSb.append("Date:" + SPACE + currentDateTime + LINE_SEPARATOR) ;
		responseSb.append("OS:" + SPACE + OS +LINE_SEPARATOR) ;
		return responseSb.toString() ;
	}


	private String frameResponseForBadReq( String version ) {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append(version + SPACE + StatusCode.BAD_REQUEST  +LINE_SEPARATOR) ;
		responseSb.append("Date:" + SPACE + currentDateTime +LINE_SEPARATOR) ;
		responseSb.append("OS:" + SPACE + OS + LINE_SEPARATOR) ;
		return responseSb.toString() ;
	}

	private String frameResponseForInValidVersion(String version) {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append(version + SPACE + StatusCode.P2P_CI_VERSION_NOT_SUPPORTED + LINE_SEPARATOR) ;
		responseSb.append("Date:" + SPACE + currentDateTime + LINE_SEPARATOR) ;
		responseSb.append("OS:" + SPACE + OS + LINE_SEPARATOR) ;
		return responseSb.toString() ;
	}

	public void removePeer( String uploadHostName, int uploadPortNo ){

		// 1. Remove the RFC from the RFC
		Iterator<RFC> itr = rfcList.iterator() ;
		while( itr.hasNext()){
			RFC currentRFC = itr.next() ;
			if( currentRFC.getHostName().equals(uploadHostName)){
				itr.remove() ;
			}
		}

		// 2. Remove the peer entry.
		Iterator<PeerRecord> peerItr = activePeers.iterator() ;
		while( itr.hasNext()){
			PeerRecord currentPeer = peerItr.next() ;
			if( currentPeer.getHostName().equals(uploadHostName) && (currentPeer.getPortNumber() == uploadPortNo)){
				peerItr.remove() ;
			}
		}
	}

	// To add a locally available RFC to the server’s index
	public String add( int rfcNo, String rfcTitle, String uploaderHostName, int uploaderPort ) {		

		boolean isActive = false ;
		for( PeerRecord peer : activePeers){
			if( peer.getHostName().equals(uploaderHostName) && peer.getPortNumber() == uploaderPort){
				isActive = true ;
			}
		}

		if( !isActive){
			// Create a new peer record
			PeerRecord peer = new PeerRecord(uploaderHostName, uploaderPort, true) ;
			// Insert it into the list of active records.
			activePeers.addFirst(peer) ;
		}
		// Add RFC contained in the request to the list of RFC
		//		RFC rfc = new RFC( rfcNo, rfcTitle, uploaderHostName ) ;
		RFC rfc = new RFC( rfcNo, rfcTitle, uploaderHostName, uploaderPort ) ;
		rfcList.add(rfc) ;
		String response = frameServerResponseForAdd( rfcNo, rfcTitle, uploaderHostName, uploaderPort ) ;
		return response ;
	}

	private String frameServerResponseForAdd(int rfcNo, String rfcTitle,
			String uploaderHostName, int uploaderPort) {

		String serverResponse = VERSION +  SPACE + StatusCode.OK.toString() +  LINE_SEPARATOR +
				"RFC" + SPACE + rfcNo + SPACE + rfcTitle + SPACE + uploaderHostName + SPACE + 
				uploaderPort + LINE_SEPARATOR ; 

		return serverResponse ;
	}

	// To find peers that have the specified RFC
	public String lookup( int rfcNo ) {
		List<PeerRecord> peersContainingRFC = new ArrayList<PeerRecord>() ; 

		String rfcTitle = "" ;
		for( RFC rfc : rfcList ){
			if( rfc.getRfcNo() == rfcNo){
				int portno = rfc.getPort() ;
				String uploaderHostName = rfc.getHostName() ;
				for( PeerRecord peer : activePeers){
					if( peer.getHostName().equals(uploaderHostName) && (peer.getPortNumber() == portno)){
						peersContainingRFC.add(peer) ;
						rfcTitle = rfc.getRfcTitle() ;
					}
				}
				
			}
		}
		String response = frameServerResponseForLookup( rfcNo, rfcTitle, peersContainingRFC ) ;
		System.out.println(response);
		return response ;
	}


	public String frameServerResponseForLookup( int rfcNo, String rfcTitle, List<PeerRecord> peersContainingRFC ) {

		StatusCode statusCode = null ;
		if( peersContainingRFC.size() > 0 ){
			statusCode = StatusCode.OK ;
		}else{
			statusCode = StatusCode.NOT_FOUND ;
		}

		StringBuilder serverResponseSb = new StringBuilder( VERSION +  SPACE + statusCode +  LINE_SEPARATOR ) ;

		for( PeerRecord peerRecord : peersContainingRFC){
			serverResponseSb.append("RFC" + SPACE +  rfcNo +  SPACE + rfcTitle ) ;
			serverResponseSb.append(SPACE + peerRecord.getHostName() + SPACE + peerRecord.getPortNumber() + LINE_SEPARATOR) ;
		}
		return serverResponseSb.toString() ;
	}

	//To request the whole index of RFCs from the server.
	public String list(){
		String serverResponse = frameServerResponseForList() ;
		System.out.println(serverResponse);
		return serverResponse ;
	}

	//	public String frameServerResponseForList() {
	//		StringBuilder sb = new StringBuilder() ;  
	//		sb.append(VERSION +  SPACE ) ;
	//		StatusCode statusCode ;
	//		if(rfcList != null && rfcList.size() > 0 ){
	//			//			statusCode = StatusCode.OK ;
	//			sb.append( StatusCode.OK.toString() +  LINE_SEPARATOR ) ;
	//		}
	//		//		else if(rfcList.size() == 0){
	//		//			sb.append( " RFC List EMPTY "  +  LINE_SEPARATOR ) ;
	//		//		}
	//		else{
	//			statusCode = StatusCode.NOT_FOUND ;
	//			sb.append( StatusCode.NOT_FOUND.toString() +  LINE_SEPARATOR ) ;
	//		}
	//		//		sb.append( statusCode.toString() +  LINE_SEPARATOR ) ;
	//
	//		for( RFC rfc : rfcList ){
	//
	//			int rfcNo = rfc.getRfcNo() ;
	//			Set<HostDetails> uploadServerNamePortMap = new HashSet<HostDetails>() ;
	//			uploadServerNamePortMap = getUploadServerDetailsForRFC( rfcNo ) ;
	//
	////			for (Map.Entry<String, Integer> entry : uploadServerNamePortMap.entrySet())
	//			for (HostDetails entry : uploadServerNamePortMap)
	//			{
	//				String hostName = entry.hostname ;
	//				String portNo = String.valueOf(entry.port);
	//				sb.append( "RFC" + SPACE + rfcNo + getRFCTitleForRFCNo(rfcNo) +  SPACE +  hostName + SPACE + portNo + LINE_SEPARATOR );
	//			}
	//		}
	//		return sb.toString() ;
	//	}


	public String frameServerResponseForList() {
		StringBuilder sb = new StringBuilder() ;  
		sb.append(VERSION +  SPACE ) ;
		StatusCode statusCode ;
		if(rfcList != null && rfcList.size() > 0 ){
			sb.append( StatusCode.OK.toString() +  LINE_SEPARATOR ) ;
		}
		else{
			statusCode = StatusCode.NOT_FOUND ;
			sb.append( StatusCode.NOT_FOUND.toString() +  LINE_SEPARATOR ) ;
		}

		for( RFC rfc : rfcList ){
			sb.append( "RFC" + SPACE + rfc.getRfcNo() + getRFCTitleForRFCNo(rfc.getRfcNo()) +  SPACE +  rfc.getHostName() + 
					SPACE + rfc.getPort() + LINE_SEPARATOR );
		}

		//				sb.append( "RFC" + SPACE + rfcNo + getRFCTitleForRFCNo(rfcNo) +  SPACE +  hostName + SPACE + portNo + LINE_SEPARATOR );

		return sb.toString() ;
	}


	/**
	 * Given the RFC Number, this method returns the entire list of upload servers ( hostname + port) that contain
	 * that rfc. 
	 * @param rfcNo
	 * @return
	 */
	private Set<HostDetails> getUploadServerDetailsForRFC(int rfcNo) {

		Set<HostDetails> set = new HashSet<HostDetails>() ;
		//		Map<String, Integer> uploadServerNamePortMap = new HashMap<String, Integer>() ;
		for( RFC rfc : rfcList){
			// 2 step process: First traverse list of rfcs using rfcNo & get the hostname
			if( rfcNo == rfc.getRfcNo()){
				// Next, traverse the list of activePeers using hostname & return that peer record
				String uploaderHostName = rfc.getHostName() ;

				for( PeerRecord peer : activePeers){
					if( peer.getHostName().equals(uploaderHostName)){
						//						uploadServerNamePortMap.put(uploaderHostName, peer.getPortNumber() ) ;
						set.add(new HostDetails( rfcNo, uploaderHostName, peer.getPortNumber())) ;
					}
				}

			}
		}


		return set ;
	}

	private String getRFCTitleForRFCNo( int rfcNo) {

		String rfcTitle = "" ;
		for( RFC rfc : rfcList){
			if( rfcNo == rfc.getRfcNo()){
				rfcTitle = rfc.getRfcTitle() ;
			}
		}
		return rfcTitle ;
	}

}

class RFC {

	private int rfcNo ;	// representative of the RFC.
	private String rfcTitle ;	// title of the RFC
	private String hostName ;	// the hostname of the peer that contains the RFC.
	private int port ;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getRfcNo() {
		return rfcNo;
	}

	public void setRfcNo(int rfcNo) {
		this.rfcNo = rfcNo;
	}

	public String getRfcTitle() {
		return rfcTitle;
	}

	public void setRfcTitle(String rfcTitle) {
		this.rfcTitle = rfcTitle;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public RFC(int rfcNo, String rfcTitle, String hostName, int port) {
		super();
		this.rfcNo = rfcNo;
		this.rfcTitle = rfcTitle;
		this.hostName = hostName;
		this.port = port ;
	}
}


class PeerRecord {

	public PeerRecord(String hostName, int portNumber) {
		super();
		this.hostName = hostName;
		this.portNumber = portNumber;
	}

	public PeerRecord(String hostName, int portNumber, boolean isActive) {
		this(hostName, portNumber) ;
		this.isActive = isActive ;
	}

	private String hostName ;	
	private int portNumber ;
	private boolean isActive ;

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public int getPortNumber() {
		return portNumber;
	}
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

}


//enum StatusCode {
//
//	OK(200), BAD_REQUEST(400), NOT_FOUND(404), P2P_CI_VERSION_NOT_SUPPORTED(505);
//
//	private int value ;
//
//
//	/**
//	 * Private constructor.
//	 */
//	private StatusCode( int value ) {
//
//		this.value = value ;
//	}
//
//	public String toString(){
//		return value + " " + name() ; 
//	}
//}

class HostDetails implements Comparable<HostDetails>{
	int rfcno ;
	String hostname ;
	int port ;
	public HostDetails(int rfcNo2, String uploaderHostName, int portNumber) {
		this.rfcno = rfcNo2 ;
		this.hostname = uploaderHostName ;
		this.port = portNumber ;
	}
	public int compareTo(HostDetails o) {

		if( rfcno == o.rfcno && this.hostname.equals(o.hostname) && (this.port == port))
			return 0;
		else
			return 1 ;
	}

	//	public String toString(){
	//		
	//	}
}
