package ru.i_novus.ms.rdm.sync.api.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import jakarta.ws.rs.QueryParam;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lgalimova
 * @since 28.02.2019
 */
@Getter
@Setter
@ApiModel("Критерий поиска записей журнала")
public class LogCriteria extends RestCriteria {

    @ApiModelProperty("Код справочника")
    @QueryParam("refbookCode")
    private String refbookCode;

    @ApiModelProperty("Дата записи журнала")
    @QueryParam("date")
    private LocalDate date;

    protected List<Sort.Order> getDefaultOrders() {
        return Collections.emptyList();
    }

    public List<Sort.Order> getOrders() {
        return (List)this.getSort().get().collect(Collectors.toList());
    }

}
