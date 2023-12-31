package spharos.user.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ReviewWriterDto {

    private Long userId;
    private String email;
    private String ImgUrl;
    private String nickname;
}
