import java.net.InetAddress;
import java.util.Scanner;

public class ConsoleMenu {

    public ConsoleMenu(){
        String again;
        Scanner sc=new Scanner(System.in);
        System.out.println("--WELCOME ON TFTP POLYTRANSFER SOFTWARE--");
        System.out.println("please select the server's IP address from which to download : ");
        String ip=sc.nextLine();
        do{
            System.out.println("please select the file's name to download : ");
            String filename=sc.nextLine();
            System.out.println("-----------------------------------------");
            try {
                ClientTFTP c=new ClientTFTP();
                int CrRv=c.receiveFile(InetAddress.getByName(ip), 69, filename);
                System.out.println("CrRv : "+CrRv);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            System.out.println("-----------------------------------------");
            System.out.println("do you want to download another file? (yes?) : ");
            again=sc.nextLine();
            System.out.println("-----------------------------------------");
        }while(again.equals("y") || again.equals("yes") || again.equals("o") || again.equals("oui"));
        System.out.println("--CLOSING TFTP POLYTRANSFER SOFTWARE--");
    }
}
