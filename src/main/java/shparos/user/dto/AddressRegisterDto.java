package shparos.user.dto;

import lombok.*;
import shparos.user.domain.User;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRegisterDto {

    private User user;
    private String localAddress;
    private String extraAddress;
    private Boolean defaultAddress;
    private Integer localCode;

}