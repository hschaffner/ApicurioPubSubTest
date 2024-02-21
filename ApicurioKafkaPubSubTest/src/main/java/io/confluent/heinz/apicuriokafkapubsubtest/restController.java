package io.confluent.heinz.apicuriokafkapubsubtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

//import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;


@org.springframework.web.bind.annotation.RestController
@RequestMapping(value = "/")
public class restController {
    @Autowired
    private Environment env;

    private final Log logger = LogFactory.getLog(restController.class);

    private int counter = 0;

    private ConfluentSession kafkaSession = null;

    //private KafkaSession kafkaSession = new KafkaSession(env);
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void postMessage(@RequestBody JsonMsg request,
                            HttpServletRequest httpRequest) {


        if(kafkaSession == null) {

            kafkaSession = new ConfluentSession(env);

            logger.info("Started new instance of Kafka");
        }

        if (counter == 0){
            counter++;
        } else {
            counter=0;
        }


        String JsonStr = "";
        ObjectMapper mapper = new ObjectMapper();
        //Output the POST message to confirm what was received
        try {
            JsonStr = mapper.writeValueAsString(request);
        } catch (JsonProcessingException je){
            logger.info("++++++++++++++++++++JSON Error: \n:");
            je.printStackTrace();
        }
        //System.out.println("JSON POST Request: " + JsonStr);
        logger.info(String.format("JSON REST POST Data -> %s " , JsonStr));
        System.out.println("JSON REST POST Data -> " + JsonStr);

        try {
            kafkaSession.sendAvroMessage(request);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/logtest")
    public String index() {
        final Log logger = LogFactory.getLog(getClass());

        logger.trace("A TRACE Message");
        logger.debug("A DEBUG Message");
        logger.info("An INFO Message");
        logger.warn("A WARN Message");
        logger.error("An ERROR Message");

        return "Howdy! Check out the Logs to see the output...";
    }
}
