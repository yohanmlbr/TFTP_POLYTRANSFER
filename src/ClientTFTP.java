import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class ClientTFTP {
    private DatagramSocket ds;
    private DatagramPacket envoi,reception;
    private static String chemin_dest="D:\\Desktop\\";

   /*
    CrRv ERRORS of receiveFile() :

        0 -> success

        -1 -> file already exist in destination folder

        1 -> error from pumpkin (acces denied, file not found...)
        2 -> connection error (lost or timed out)
    */

    public int sendACK(){
        try {
            byte[] data = {0,4,reception.getData()[2],reception.getData()[3]};  //new byte[4];
            envoi=new DatagramPacket(data,data.length,reception.getAddress(),reception.getPort());
            ds.send(envoi);
            int index=(reception.getData()[3]&0xFF)+(reception.getData()[2]&0xFF)*256;
            System.out.println("ACK#"+index+" sent");
            return index;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void sendRRQ(String nom_fichier, InetAddress ip_serv, int port_serv){
        int i_data = 0;
        byte[] data;
        try {
            //initialisation RRQ
            data = new byte[516];
            data[i_data] = 0;
            i_data++;
            data[i_data] = 1;
            i_data++;
            for (int i = 0; i < nom_fichier.getBytes().length; i++) {
                data[i_data] = nom_fichier.getBytes()[i];
                i_data++;
            }
            data[i_data] = 0;
            i_data++;
            String mode = "netascii";
            for (int i = 0; i < mode.getBytes().length; i++) {
                data[i_data] = mode.getBytes()[i];
                i_data++;
            }
            data[i_data] = 0;
            //fin construction msg, envoi
            envoi = new DatagramPacket(data, data.length, ip_serv, port_serv);
            ds.send(envoi);
            System.out.println("RRQ sent");
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public int receiveDATA(){

        try {
            byte[] data = new byte[516];
            reception = new DatagramPacket(data, data.length);
            System.out.println("waiting for DATA...");
            ds.receive(reception);
            if(reception.getData()[1]==5){
                String msg=new String(reception.getData(),4,reception.getLength()-4);
                System.out.println("ERROR : "+msg);
                return -1;
            }
            int index=(reception.getData()[3]&0xFF)+(reception.getData()[2]&0xFF)*256;
            System.out.println("DATA#"+index+" received");
            return index;
        } catch (IOException e) {
            System.out.println("ERROR : Time out or connection lost");
            return -2;
        }
    }

    public int checkReceive(int r){
        if(r==-1){
            return 1;
        }
        if(r==-2){
            return 2;
        }
        else{
            return 0;
        }
    }

    public void writeFile(FileOutputStream fea){
        try {
            fea.write(reception.getData(),4,reception.getLength()-4);
            System.out.println("writing on the new file");
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public int checkFileExistence(String nom_fichier){
        File f=new File(chemin_dest + nom_fichier);
        if(f.isFile()) {
            System.out.println("ERROR : File already exist in destination folder");
            return -1;
        }
        else
            return 0;
    }

    public void abortTransfer(String nom_fichier){
        File f=new File(chemin_dest + nom_fichier);
        f.delete();
    }

    public int receiveFile(InetAddress ip_serv, int port_serv, String nom_fichier) {
            try {
                System.out.println("initialisation of "+nom_fichier+"'s transfer");
                if(checkFileExistence(nom_fichier)==-1){
                    System.out.println("transfert aborted...");
                    return -1;
                }
                int r,s;
                ds = new DatagramSocket();
                ds.setSoTimeout(30000);                                //30 secondes pour le time out, comme pumpkin pour grant access
                sendRRQ(nom_fichier, ip_serv, port_serv);
                r=receiveDATA();
                if(checkReceive(r)!=0){
                    System.out.println("transfert aborted...");
                    return checkReceive(r);
                }
                FileOutputStream fea = new FileOutputStream(chemin_dest + nom_fichier);
                writeFile(fea);
                s=sendACK();
                while(reception.getLength()==516){
                    r=receiveDATA();
                    if(checkReceive(r)!=0){
                        System.out.println("transfert aborted...");
                        fea.close();
                        abortTransfer(nom_fichier);
                        return checkReceive(r);
                    }
                    if(r==s+1){
                        writeFile(fea);
                        s=sendACK();
                    }
                    else{
                        System.out.println("ERROR : DATA Block#"+r+" doesn't correspond to ACK Block#"+(s+1)+" expected");
                        System.out.println("transfert aborted...");
                        fea.close();
                        abortTransfer(nom_fichier);
                        return 2;
                    }
                }
                fea.close();
                System.out.println(nom_fichier+"'s transfer successfully ended");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return 0;
    }

}
