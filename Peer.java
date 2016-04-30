
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class Peer {

	private static String serverName  ;
	private static int serverPort = 7734 ;

	public Peer(){
		//		serverName = "localhost" ;
		//		serverPort = 7734 ;
	}

	public static final String VERSION  = "P2P-CI/1.0" ;
	public static final String TAB  = " " ;
	public static final String OS = System.getProperty("os.name") ; 
	public static final String LINE_SEPARATOR = "\r\n" ;

	@SuppressWarnings("resource")
	public static void main( String args[] ) throws IOException{

		Socket clientSocket ;
		Scanner sc  = new Scanner( System.in ) ;
		System.out.println("Please enter the server name (IP Address) :"); 
		String serverName = sc.nextLine() ;
		//try{
		//if( args.length < 1){
		//	System.out.println("Insufficient arguments, Please provide the server name.");
		//}
		//}catch( ArrayIndexOutOfBoundsException e){
		//			System.out.println("Insufficient arguments, Please provide the server name.");
		//}
		//		String serverName = args[0] ;
		System.out.println("Server Details :\nAddress : " + serverName + "\nPort : " + serverPort );
		clientSocket = new Socket(serverName, 7734) ;
		System.out.println("Do you have any RFCs ? Y/N");
		boolean hasRFC = false ;
		String ans = "" ;
		do{
			ans = sc.nextLine().toUpperCase() ;
			if( "Y".equals(ans) ){
				hasRFC = true ;
				break ;
			}else if( ("N".equals(ans))) {
				hasRFC = false ;
				break ;
			}else{
				System.out.println("Please enter Y/N.");
				continue ;
			}
		}while( !ans.equals("Y") || !ans.equals("N")  );

		String uploaderHostName = "" ;
		int uploadServerPort ;
		int rfcCount ;
		int rfcNo = 0 ;

		OutputStream out;
		DataOutputStream dos;
		InputStream in;
		DataInputStream dis;
		String request = "" ;
		Socket downloadClientSocket = null ;

		System.out.println("Please enter the Host Name :");	// This Host name
		uploaderHostName = sc.nextLine() ;

		System.out.println("Please enter the Upload Server port :");
		uploadServerPort = sc.nextInt();

		// Spawn a  new thread for the Upload Server Thread.
		ServerSocket uploadServerSocket = new ServerSocket( uploadServerPort ) ;
		Thread uploadServerThread = new Thread( new UploadServerThread(uploadServerSocket)) ;
		uploadServerThread.start() ;

		if( hasRFC){

			// One time affair when a client first joins the network.
			System.out.println("How many RFCs do you have?");
			rfcCount = sc.nextInt();

			System.out.println("Please provide the RFC Numbers.");
			for( int i = 1 ; i <= rfcCount ; i++ ){
				System.out.println("RFC No:" + i);
				rfcNo = sc.nextInt() ;

				// Register this peer to the Main Server, 1 ADD Request per RFC number
				request = frameAddRequest(uploaderHostName, uploadServerPort, rfcNo) ;
				System.out.println("\nRequest formed : \n" + request);
				out = clientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				dos.writeUTF(request) ;
				in = clientSocket.getInputStream();
				dis = new DataInputStream(in);
				String response = dis.readUTF();
				System.out.println("\nResponse from the server is \n" + response);
				System.out.println("-------------------------------------------------------------------------------");
			}
		}
		System.out.println("-------------------------------------------------------------------------------");
		int option ;
		do{
			System.out.println("Please input the option\n1 : ADD\n2 : LOOKUP\n3 : LIST\n4 : GET\n5 : EXIT");
			option = sc.nextInt() ;

			switch( option ){
			case 1 :
				//				if( !hasRFC ){
				//					System.out.println("No RFC to add.");
				//					continue ;
				//				}
				System.out.println("Please enter RFC no:");
				sc = new Scanner(System.in) ;
				rfcNo = sc.nextInt() ;
				out = clientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				request = frameAddRequest(uploaderHostName, uploadServerPort, rfcNo) ;
				System.out.println("\nRequest formed : \n" + request);
				dos.writeUTF(request) ;
				break ;
			case 2 :
				out = clientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				request = frameLookUpRequest(uploaderHostName, uploadServerPort) ;
				System.out.println("\nRequest formed : \n" + request);
				dos.writeUTF(request) ;
				break ;

			case 3 :
				out = clientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				request = frameListRequest(uploaderHostName, uploadServerPort, rfcNo) ;
				System.out.println("\nRequest formed : \n" + request);
				dos.writeUTF(request) ;
				break ;

			case 4 :
				int rfcServerPort = 0 ;
				String rfcServerName = "" ;
				System.out.println("\nWhich RFC file( RFC No) do you wish to download?");
				Scanner sc1 = new Scanner(System.in) ;
				rfcNo = Integer.parseInt(sc1.nextLine()) ;

				System.out.println("\nEnter the hostname from which you want to download.");
				rfcServerName = sc1.nextLine() ;

				System.out.println("\nEnter the port from which you want to download.");
				rfcServerPort = Integer.parseInt(sc1.nextLine()) ;

				downloadClientSocket = new Socket(rfcServerName, rfcServerPort ) ;
				out = downloadClientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				request = frameGetRequest( rfcNo, rfcServerName) ;
				System.out.println("\nRequest formed : \n" + request);
				dos.writeUTF(request) ;

				// Process the incoming response
				in = downloadClientSocket.getInputStream() ;
				dis = new DataInputStream(in) ;
				String respo = dis.readUTF();
				System.out.println("\nResponse from the Server :\n " + respo);
				System.out.println("-------------------------------------------------------------------------------");
				createRFCFile(respo, rfcNo) ;
				continue ;

			case 5 :
				out = clientSocket.getOutputStream();
				dos = new DataOutputStream(out);
				request = frameExitRequest( uploaderHostName, uploadServerPort) ;
				System.out.println("\nRequest formed : \n" + request);
				dos.writeUTF(request) ;
				break ;

			default:
				System.out.println("\nPlease Enter a valid option. Try Again.");
				continue ;
			}

			in = clientSocket.getInputStream();
			dis = new DataInputStream(in);
			String response = dis.readUTF();
			System.out.println("\nResponse from the server is \n" + response);
			System.out.println("-------------------------------------------------------------------------------");

			if( option == 5){
				if( in != null)
					in.close() ;
				if( dis != null)
					dis.close() ;
				if( out != null)
					out.close() ;
				if( dos != null)
					dos.close() ;
				if( downloadClientSocket != null){
					downloadClientSocket.close() ;
				}
				clientSocket.close() ;
				break ;
			}
		}while( option != 5) ;
		System.out.println("\nClosed connection with the server.");
		System.out.println("-------------------------------------------------------------------------------");
		System.exit(0) ;
	}


	/**
	 * This method processes the incoming response message from the server 
	 * and creates the rfcNo.txt file on this peer machine.
	 * @param responseMessage
	 * @param rfcNo
	 */
	private static void createRFCFile(String responseMessage , int rfcNo) {
		String lines[] = responseMessage.split(LINE_SEPARATOR) ;
		String words[] = lines[0].split(TAB) ;
		String statusCode = words[1] ;
		BufferedWriter bw = null;

		if( statusCode.equals("200")){

			File dir = new File("downloadedRFC") ;
			if( !dir.exists()){
				dir.mkdir() ;
			}

			File file = new File(dir.getAbsolutePath() + File.separator + rfcNo + ".txt") ;
			try {	
				file.createNewFile() ;
			} catch (IOException e1) {
				System.out.println("\nException occured while creating a file.");
			}

			try{
				bw = new BufferedWriter(new FileWriter(file)) ;
				int lineNo = 6 ;
				while( lineNo < lines.length){
					bw.write(lines[ lineNo ] ) ;
					bw.append(LINE_SEPARATOR) ;
					lineNo++ ;
				}
			}catch( IOException ioe){
				ioe.printStackTrace() ;
			}finally{
				try {
					bw.close() ;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	private static String frameExitRequest(String uploaderHostName, int uploadServerPort) {

		String request = "EXIT" + TAB +  LINE_SEPARATOR +
				"Host:" + TAB + uploaderHostName +  LINE_SEPARATOR +
				"Port:" +  TAB + uploadServerPort + LINE_SEPARATOR  ;

		return request;
	}
	/**
	 *  GET RFC 1234 P2P-CI/1.0
		Host: somehost.csc.ncsu.edu
		OS: Mac OS 10.4.1
	 * @return
	 */
	private static String frameGetRequest( int rfcNo, String hostName ) {
		String request = "GET" + TAB + "RFC" + TAB + rfcNo + TAB + VERSION + LINE_SEPARATOR +
				"Host:" + TAB + hostName +  LINE_SEPARATOR +
				"OS:" +  OS + TAB + LINE_SEPARATOR  ;
		return request;
	}

	private static String frameAddRequest( String hostName, int uploadServerPort, int rfcNo ) {

		System.out.println("\nPlease input the RFC Title ") ;
		Scanner sc = new Scanner( System.in) ;
		String rfcTitle = sc.nextLine() ;
		String request = "ADD" + TAB + "RFC" + TAB + rfcNo +  TAB + VERSION + LINE_SEPARATOR +
				"Host:" + TAB + hostName +  LINE_SEPARATOR +
				"Port:" + TAB + uploadServerPort + LINE_SEPARATOR +
				"Title:" + TAB + rfcTitle + LINE_SEPARATOR  ; 
		return request;
	}

	private static String frameLookUpRequest( String hostName, int uploadServerPort ) {
		System.out.println("\nPlease input the RFC Title ") ;
		Scanner sc = new Scanner( System.in) ;
		String rfcTitle = sc.nextLine() ;
		System.out.println("\nPlease input the RFC No to look up ") ;
		int rfcNo = sc.nextInt() ;

		String request = "LOOKUP" + TAB + "RFC" + TAB + String.valueOf(rfcNo) + TAB + VERSION + LINE_SEPARATOR +
				"Host:" + TAB + hostName +  LINE_SEPARATOR +
				"Port:" + TAB + uploadServerPort + LINE_SEPARATOR +
				"Title:" + TAB + rfcTitle + LINE_SEPARATOR  ; 

		return request;
	}

	private static String frameListRequest( String hostName, int uploadServerPort, int rfcNo ) {
		String request = "LIST_ALL" + TAB + VERSION + LINE_SEPARATOR +
				"Host:" + TAB + hostName +  LINE_SEPARATOR +
				"Port:" + TAB + uploadServerPort + LINE_SEPARATOR  ;
		return request;
	}
}

class UploadServerThread extends Thread {

	public static final String VERSION = "P2P-CI/1.0" ;
	public static final String OS = System.getProperty("os.name") ;
	public static final String TAB = " " ; 
	public static final String LINE_SEPARATOR = "\r\n" ;
	public static Date currentDateTime = new Date() ;
	public static String CONTENT_TYPE = "text/text" ;

	ServerSocket serverSocket = null;	//hostname + upload server port
	Socket rfcRequestSocket = null;
	InputStreamReader is = null;
	PrintStream ps = null;
	BufferedReader br = null;

	// Constructor
	public UploadServerThread(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;	
	}

	public UploadServerThread (int portNo) {
		try {
			this.serverSocket = new ServerSocket(portNo) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run(){
		try {
			OutputStream out;
			DataOutputStream dos;

			InputStream in;
			DataInputStream dis;

//			rfcRequestSocket = serverSocket.accept();

			while (true) {
				rfcRequestSocket = serverSocket.accept();
				// Process the RFC request
				// Process the incoming response
				in = rfcRequestSocket.getInputStream() ;
				dis = new DataInputStream(in) ;
				String request = dis.readUTF();
				System.out.println("Inside Upload Server:\n" + request);
				String[] lines = request.split(LINE_SEPARATOR) ;
				String firstLine = lines[0] ; 
				String words[] = firstLine.split(TAB) ;
				String rfcNum = words[2] ;
				String version = words[3] ; 
				// call processRequest				
				String response = frameGetResponseMessage( rfcNum, version) ;
				out = rfcRequestSocket.getOutputStream();
				dos = new DataOutputStream(out);
				System.out.println("Sending the response to the peer.\nResponse is \n" + response );
				dos.writeUTF(response) ;
				rfcRequestSocket.close() ;
			}
		} 
		//		catch(FileNotFoundException fnfe){
		//			System.out.println(fnfe);
		//		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	public static String frameGetResponseMessage( String rfcNum, String version ) throws IOException{

		String fileSeparator = System.getProperty("file.seperator") ;
		String fileName = "/home/prutha/Desktop/IP/project/Internet-Protocols---Project1/IP project 1/src/sourceRFC" + "/" +  rfcNum + ".txt" ;
				//		String fileName = "./" + "sourceRFC" + "/" +  rfcNum + ".txt" ;
				File f = null ;
		String response = "" ;
		StatusCode statusCode = null ;

		try{
			f = new File(fileName) ;

			if(! VERSION.equals(version)){
				statusCode = StatusCode.P2P_CI_VERSION_NOT_SUPPORTED ;
				response = frameResponseForInValidVersion(version) ;
			}else {

				if( f.exists() && !f.isDirectory()){

					long lastModifiedDateTime = f.lastModified() ; 
					String fileContent = readFileContents(f) ;
					long contentLength = f.length() ;
					statusCode = StatusCode.OK ;
					response = VERSION + TAB + statusCode.toString() + LINE_SEPARATOR +
							"Date:" + TAB +  currentDateTime + LINE_SEPARATOR +
							"OS:" + TAB + OS + LINE_SEPARATOR +
							"Last-Modified:" + TAB + lastModifiedDateTime + LINE_SEPARATOR +
							"Content-Length:" +  TAB + contentLength + LINE_SEPARATOR +
							"Content-Type:" + TAB + CONTENT_TYPE +  LINE_SEPARATOR + 
							fileContent + LINE_SEPARATOR ;

				}else if( !f.exists() ||  f.isDirectory()) {
					System.out.println("Inside Peer: file not found" );
					statusCode = StatusCode.NOT_FOUND ;
					response = frameResponseForFileNotFound( version) ;
				}else{
					statusCode = StatusCode.BAD_REQUEST ;
					response = frameResponseForBadReq(version) ;
				}

			}
		}catch(FileNotFoundException fnfe){
			System.out.println("Inside FNFE catch : Peer");
			statusCode = StatusCode.NOT_FOUND ;
			response = frameResponseForFileNotFound( version) ;
			return response ;
		}
		return response ;
	}

	private static String frameResponseForBadReq( String version ) {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append(version + TAB + StatusCode.BAD_REQUEST  +TAB) ;
		responseSb.append("Date:" + TAB + currentDateTime +TAB) ;
		responseSb.append("OS:" + TAB + OS +TAB) ;
		return responseSb.toString() ;
	}

	private static String frameResponseForFileNotFound( String version ) {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append(version + TAB + StatusCode.NOT_FOUND  +TAB) ;
		responseSb.append("Date:" + TAB + currentDateTime +TAB) ;
		responseSb.append("OS:" + TAB + OS +TAB) ;
		return responseSb.toString() ;
	}

	private static String frameResponseForInValidVersion(String version) {
		StringBuilder responseSb = new StringBuilder() ;
		responseSb.append(version + TAB + StatusCode.P2P_CI_VERSION_NOT_SUPPORTED +TAB) ;
		responseSb.append("Date:" + TAB + currentDateTime +TAB) ;
		responseSb.append("OS:" + TAB + OS +TAB) ;
		return responseSb.toString() ;
	}

	// Read the file
	private static String readFileContents( File f ) throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(f)) ;
		StringBuilder fileContentSb = new StringBuilder();
		String line ;

		while( (line = br.readLine()) != null){
			fileContentSb.append(line) ;
			fileContentSb.append(LINE_SEPARATOR) ;
		}
		br.close() ;
		return fileContentSb.toString() ;
	}
}



enum StatusCode {

	OK(200), BAD_REQUEST(400), NOT_FOUND(404), P2P_CI_VERSION_NOT_SUPPORTED(505);

	private int value ;


	/**
	 * Private constructor.
	 */
	private StatusCode( int value ) {

		this.value = value ;
	}

	public String toString(){
		return value + " " + name() ; 
	}
}
