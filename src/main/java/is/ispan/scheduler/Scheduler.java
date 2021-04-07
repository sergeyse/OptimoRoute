package is.ispan.scheduler;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestTemplate;



@Component
public class Scheduler {

    String responseForController ;

    @Value("${path.input-xmls}")
    private String inputXmlsPath;

    @Value("${path.processed-xmls}")
    private String processedXmlsPath;

    @Value("${path.failed-xmls}")
    private String failedXmlsPath;

    @Value("${path.processed-ascii}")
    private String processedAsciiPath;

    @Value("${dk.token}")
    private String dkToken;
 
    @Autowired
    private JdbcTemplate jdbcTemplate;

 
 //   @Scheduled(cron = "${cron.generate-ascii}")



    public String generateASCIIFiles() throws IOException {

        responseForController = "";



        Map<String, String> dkAccountingCustomerMap = new HashMap<String, String>();

        String sql = "SELECT DISTINCT LiOrder.auf_kopf.auf_nr, LiOrder.auf_kopf.bestell_nr, LiOrder.auf_kopf.kunr, cust_ord_addr.name, LiOrder.auf_kopf.kopf_tour,  FORMAT( LiOrder.auf_kopf.liefer_dat, 'yyyy-MM-dd' ),   auf_adr_deliv.adr1, LiOrder.kust.ku_art, auf_stat.stat_liefer_art, order_totals.order_tot_qty, ordstat.status_code, auf_kopf.auf_off, auf_adr_deliv.adr2, auf_adr_deliv.adr3, auf_adr_deliv.tel, auf_adr_deliv.plz\n" +
                "FROM LiOrder.auf_kopf\n" +
                "  LEFT OUTER JOIN LiOrder.auf_adr auf_adr_deliv ON LiOrder.auf_kopf.auf_nr = auf_adr_deliv.auf_nr AND auf_adr_deliv.adr_art IN (1)\n" +
                "  LEFT OUTER JOIN LiOrder.auf_pos ON LiOrder.auf_kopf.auf_nr = LiOrder.auf_pos.auf_nr\n" +
                "  INNER JOIN LiOrder.auf_stat ON LiOrder.auf_pos.auf_nr = auf_stat.auf_nr AND auf_pos.auf_pos = auf_stat.auf_pos AND auf_pos.variante = auf_stat.variante\n" +
                "  LEFT OUTER JOIN LiOrder.auf_adr cust_ord_addr ON auf_kopf.auf_nr = cust_ord_addr.auf_nr AND cust_ord_addr.adr_art IN (0)\n" +
                "  LEFT OUTER JOIN LiOrder.kust ON LiOrder.auf_kopf.kunr = kust.kunr AND kust.vk_ek IN (0)\n" +
                "  LEFT OUTER JOIN LiOrder.order_totals ON LiOrder.auf_kopf.auf_nr = order_totals.order_no\n" +
                "  LEFT OUTER JOIN LiOrder.ordstat ON LiOrder.auf_kopf.auf_nr = ordstat.order_no AND ordstat.item_no IN (0) AND ordstat.pane_no IN (0) AND ordstat.comp_no IN (0) AND ordstat.activ IN (1)\n" +
                "WHERE ( ( (auf_stat.stat_liefer_art IN (2))) AND ( (LiOrder.auf_kopf.auf_off IN (0))) AND ( (0 = (SELECT SUM(LIEFER_ANZ) FROM LiOrder.auf_stat a WHERE a.auf_nr = auf_stat.auf_nr AND a.variante = auf_stat.variante ) AND LiOrder.auf_kopf.auf_nr NOT IN (SELECT auf_nr FROM LiOrder.auf_stat a\n" +
                "WHERE a.auf_nr = auf_stat.auf_nr AND a.auf_pos = auf_stat.auf_pos AND a.variante = auf_stat.variante AND a.history = 0 AND a.lief_close = 1))) AND ( (ordstat.status_code IN (133, 135, 132)))) ORDER BY LiOrder.auf_kopf.auf_nr, LiOrder.auf_kopf.bestell_nr, LiOrder.auf_kopf.kunr,cust_ord_addr.name, LiOrder.auf_kopf.kopf_tour,  FORMAT( LiOrder.auf_kopf.liefer_dat, 'yyyy-MM-dd' ), auf_adr_deliv.adr1, kust.ku_art, auf_stat.stat_liefer_art, order_totals.order_tot_qty, ordstat.status_code, auf_kopf.auf_off";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            public void processRow(ResultSet rs) throws SQLException {
                do {

                    System.out.println("SQL result set " + rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getString(4) + " " + rs.getString(5) + " " + rs.getString(10));


                    //TODO check existing orders
                    RestTemplate restTemplate2 = new RestTemplate();
                    String CheckOptiRouteCreateOrderURL = "https://api.optimoroute.com/v1/get_orders?key=0d005a9fa85c2e455b2828adbd3618a11QhwASVCUVM&orderNo=" + rs.getString(1);
                    HttpHeaders checkHeaders = new HttpHeaders();
                    checkHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    checkHeaders.setContentType(MediaType.APPLICATION_JSON);


                    ResponseEntity<String> checkResponse = restTemplate2.getForEntity(CheckOptiRouteCreateOrderURL, String.class);

                    ObjectMapper responceMapper = new ObjectMapper();
                    try {
                        JsonNode responceRoot = responceMapper.readTree(checkResponse.getBody());
                        System.out.println(" /n Json check " + responceRoot.get("success").asBoolean() + " For Order " + rs.getString(1));

                        if (responceRoot.get("success").asBoolean() == false) {


                            // map SQL result to JSON object
                            ObjectMapper mapper = new ObjectMapper();
                            com.fasterxml.jackson.databind.node.ObjectNode rootNode = mapper.createObjectNode();

                            com.fasterxml.jackson.databind.node.ObjectNode childNode1 = mapper.createObjectNode();
                            childNode1.put("address", rs.getString(13));
                            childNode1.put("locationNo", rs.getString(7));
                            childNode1.put("locationName", rs.getString(4));
                            childNode1.put("acceptPartialMatch", true);

                            rootNode.put("operation", "MERGE");
                            rootNode.put("orderNo", rs.getString(1));
                            rootNode.put("type", "D");
                            rootNode.put("date", rs.getString(6));
                            rootNode.set("location", childNode1);
                            rootNode.put("duration", 20);
                            rootNode.put("load1", 1);
                            rootNode.put("load2", 2);
                            rootNode.put("notes", "Deliver at back door");

                            String bodyJsonString = "";

                            try {
                                bodyJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                             createOptiOrder(bodyJsonString);

                        }

                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }


                } while (rs.next());
            }
        });


        return responseForController;
    }

    public String createOptiOrder(String bodyJson) {


        // TODO make REST request.
        RestTemplate restTemplate = new RestTemplate();
        String optiRouteCreateOrderURL = "https://api.optimoroute.com/v1/create_order?key=0d005a9fa85c2e455b2828adbd3618a11QhwASVCUVM";
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(bodyJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(optiRouteCreateOrderURL, HttpMethod.POST, entity, String.class);
        System.out.println("Response Body " + response.getBody());

        //TODO check "success" = false and show which order Nr. has not been accepted. {"message":"Location is not valid","code":"ERR_LOC_NOT_VALID","success":false}

        responseForController = response.getBody()+responseForController;

        return response.getBody();

    }

}
