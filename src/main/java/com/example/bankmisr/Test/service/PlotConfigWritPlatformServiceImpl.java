package com.example.bankmisr.Test.service;

import com.example.bankmisr.Test.data.PlotConfigModel;
import com.example.bankmisr.Test.domain.PlotDataConfig;
import com.example.bankmisr.Test.domain.PlotModelConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Service
public class PlotConfigWritPlatformServiceImpl implements PlotConfigWritPlatformService {

    private PlotModelConfigRepository plotModelConfigRepository;
@Autowired
    public PlotConfigWritPlatformServiceImpl(PlotModelConfigRepository plotModelConfigRepository) {
        this.plotModelConfigRepository = plotModelConfigRepository;
    }


    @Override
    @Transactional
    public void savePlotConfig(PlotConfigModel plotConfigModel) {


        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern( "h:mma ")
                .toFormatter(Locale.US);

        LocalTime time = LocalTime.parse(fmt.format(LocalTime.now()), fmt);



        PlotDataConfig plotDataConfig=new PlotDataConfig(plotConfigModel.getId(),time,plotConfigModel.getWaterAmount());
           this.plotModelConfigRepository.saveAndFlush(plotDataConfig);


            }
}
