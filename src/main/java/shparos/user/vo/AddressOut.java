package shparos.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddressOut {

    private Long id;
    private String localAddress;
    private String extraAddress;
    private Boolean defaultAddress;

}
