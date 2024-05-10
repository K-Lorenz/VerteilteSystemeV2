import communications.Cli;
import communications.Client;
import communications.MessageBroker;
import communications.TravelBroker;
import misc.PropertyLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int testAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.testamount"));
        int clientAmount = Integer.parseInt(PropertyLoader.loadProperties().getProperty("main.clientamount"));
        List<String> testingInputs = new ArrayList<>();
        for (int i = 0; i<testAmount; i++){
            testingInputs.add("book "+ randomString());
        }
        Thread mBThread = new Thread(()->{
            MessageBroker messageBroker = new MessageBroker();
            messageBroker.start();
        });
        Thread tBThread = new Thread(()->{
            TravelBroker travelBroker = new TravelBroker();
            travelBroker.start();
        });
        mBThread.start();
        tBThread.start();
        for(int i = 0; i<clientAmount; i++){
            Thread clThread = new Thread(()->{
                Client client = new Client();
                client.start(testingInputs);
            });
            clThread.start();
        }
    }
    public static String randomString(){
        int paramAmount = (int) Math.floor(Math.random()*10) + 1;
        StringBuilder builderString = new StringBuilder();
        for(int i = 0; i<paramAmount; i++){
            boolean type = Math.random() < 0.5;
            if(type){
                builderString.append("--flight");
            }else{
                builderString.append("--hotel");
            }
            builderString.append(" 'abc' " + ((int) Math.floor(Math.random()*10) + 1) + " ");
        }
        return  builderString.toString();
    }
}