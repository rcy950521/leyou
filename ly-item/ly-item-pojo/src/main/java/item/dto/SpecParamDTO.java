package item.dto;

import lombok.Data;

@Data
public class SpecParamDTO {
    private Long id;
    private Long cid;
    private Long groupId;
    private String name;
    private Boolean numeric;//是否是数字
    private String unit;
    private Boolean generic;//是否是普通的规格
    private Boolean searching;
    private String segments;
}