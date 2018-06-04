import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

//Server class
public class BlockingServer 
{
 public static void main(String[] args) throws IOException 
 {
     ServerSocket ss = new ServerSocket(19000);
      
     while (true) 
     {
         Socket s = null;      
         try
         {
             s = ss.accept();
              
             // obtaining input and out streams
             DataInputStream dis = new DataInputStream(s.getInputStream());
             DataOutputStream dos = new DataOutputStream(s.getOutputStream());
              
             // create a new thread object
             Thread t = new ClientHandler(s, dis, dos);
             t.start();
              
         }
         catch (Exception e){
             s.close();
             ss.close();
             e.printStackTrace();
         }
     }
 }
}

//ClientHandler class
class ClientHandler extends Thread 
{
 final DataInputStream dis;
 final DataOutputStream dos;
 final Socket s;
  
 public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) 
 {
     this.s = s;
     this.dis = dis;
     this.dos = dos;
 }

 @Override
 public void run() 
 {
     String received;
     while (true) 
     {
         try {
			received = dis.readLine();
			dos.writeBytes(received);
		}catch (EOFException e) {
			break;
		} 
         catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			break;
		}
     }
 }
}
