package com.example.bankmisr.Test.api;


import com.example.bankmisr.Test.domain.PlotModelConfigRepository;
import com.example.bankmisr.Test.domain.PlotModelRepository;
import com.example.bankmisr.Test.exception.RestResponseEntityExceptionHandler;
import com.example.bankmisr.Test.data.PlotConfigModel;
import com.example.bankmisr.Test.domain.PlotData;
import com.example.bankmisr.Test.domain.PlotDataConfig;
import com.example.bankmisr.Test.data.PlotModel;
import com.example.bankmisr.Test.service.PlotConfigWritPlatformService;
import com.example.bankmisr.Test.service.PlotReadPlatformService;
import com.example.bankmisr.Test.service.PlotWritPlatformService;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;


@RestController
@Scope("singleton")
public class ApiIrrigationResource {
    private PlotWritPlatformService plotWritPlatformService;
    private PlotReadPlatformService plotReadPlatformService;
    private PlotModelRepository plotModelRepository;
    private PlotConfigWritPlatformService plotConfigWritPlatformService;
    private PlotModelConfigRepository plotModelConfigRepository;
    private final RestTemplate restTemplate;
    static int counterOfRetryTimes;

    @Autowired
    public ApiIrrigationResource(final PlotModelRepository plotModelRepository,

                                 PlotWritPlatformService plotWritPlatformService,
                                 PlotReadPlatformService plotReadPlatformService,
                                 PlotConfigWritPlatformService plotConfigWritPlatformService,
                                 PlotModelConfigRepository plotModelConfigRepository,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.plotWritPlatformService = plotWritPlatformService;
        this.plotModelRepository = plotModelRepository;
        this.restTemplate = restTemplateBuilder.build();
        this.plotReadPlatformService = plotReadPlatformService;
        this.plotConfigWritPlatformService=plotConfigWritPlatformService;
        this.plotModelConfigRepository=plotModelConfigRepository;


    }

    @GetMapping("/getplots")
    @ResponseBody
    public List<PlotData> getAllPlots() {
        List<PlotData> plotDataList = this.plotReadPlatformService.getAllPlots();


        return plotDataList;

    }

    @PostMapping("/addPlot")
    @ResponseBody
    public ResponseEntity<String> AddAplot(@RequestBody PlotModel plotModel) {

        this.plotWritPlatformService.savePlot(plotModel);
        return new ResponseEntity<>("Plot Data Added Successfully", HttpStatus.CREATED);
    }


    @PostMapping("/addConfiguration")
    @ResponseBody
    public ResponseEntity<String> addConfigurationOfPlot(@RequestBody PlotConfigModel plotConfigModel) {

        this.plotConfigWritPlatformService.savePlotConfig(plotConfigModel);
        return new ResponseEntity<>("Plot Config Data Added Successfully", HttpStatus.OK);
    }

    @PutMapping("/editPlot/{id}")
    @ResponseBody
    public ResponseEntity<String> editAplot(@RequestBody PlotModel plotModel, @PathVariable("id") int id) {

        this.plotWritPlatformService.editPlot(id, plotModel);
        return new ResponseEntity<>("Plot Data editted Successfully", HttpStatus.OK);
    }

    @GetMapping("/senesorAlert")
    @Scheduled(fixedRate = 5000)
    @ResponseBody
    public ResponseEntity<Set<PlotData>> getAllPlotsScheduled() throws Exception {


        HttpHeaders headers = new HttpHeaders();
        HttpEntity request = new HttpEntity(headers);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

         try {
         ResponseEntity<PlotData[]> response=this.restTemplate.getForEntity("https://domainofsensordevice.com",PlotData[].class);

         } catch (HttpStatusCodeException ex) {
             counterOfRetryTimes++;
             if(counterOfRetryTimes>10) {

                 throw new RestResponseEntityExceptionHandler("Alert The Sensor Isn`t Available");
             }
              throw new RestResponseEntityExceptionHandler(ex.getRawStatusCode()+" "+ex.getStatusCode().toString()+" "+ex.getResponseBodyAsString());

      



        }
        Set<PlotData> plotDataList =null;

        return new ResponseEntity<Set<PlotData>>(plotDataList,HttpStatus.OK);

    }


    @RequestMapping("/irrigationIfSuccess")
    @ResponseBody
    public ResponseEntity<Set<PlotData>> doIrrigation() {


        Set<PlotData> plotDataList =null;






      plotDataList = this.plotReadPlatformService.getAllPlotSchedueled();
for(PlotData resultOfPlot : plotDataList) {
    PlotDataConfig plotDataConfig = this.plotModelConfigRepository.findById(resultOfPlot.getPlotDataConfig().getId()).orElseThrow();
    
    DateTimeFormatter fmt = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern( "h:mma ")
            .toFormatter(Locale.US);

    LocalTime time = LocalTime.parse(fmt.format(LocalTime.now()), fmt);
    if (plotDataConfig.getTimeSlot().getHour() == time.getHour() && plotDataConfig.getTimeSlot().getMinute() == time.getMinute()) {


        PlotData statusOfIrrigation = this.plotModelRepository.findById(resultOfPlot.getId()).orElseThrow();
        resultOfPlot.setActive(true);
        resultOfPlot.setIrrigationRequired(true);
         DateTimeFormatter fmtTimeOfLastIrrigation = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern( "h:mma ")
                .toFormatter(Locale.US);

        LocalTime timeOfLastIrrigation = LocalTime.parse(fmtTimeOfLastIrrigation.format(LocalTime.now()), fmtTimeOfLastIrrigation);
        resultOfPlot.setTimeOfLastIrrigation(timeOfLastIrrigation);
        this.plotModelRepository.saveAndFlush(statusOfIrrigation);


    } else {


        PlotData statusOfIrrigation = this.plotModelRepository.findById(resultOfPlot.getId()).orElseThrow();
        resultOfPlot.setActive(false);
        resultOfPlot.setIrrigationRequired(false);
        this.plotModelRepository.saveAndFlush(statusOfIrrigation);

     }



     }


        return new ResponseEntity<Set<PlotData>>(plotDataList,HttpStatus.OK);



    }

}
