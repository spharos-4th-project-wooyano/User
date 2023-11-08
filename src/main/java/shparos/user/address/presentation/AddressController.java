package shparos.user.address.presentation;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import shparos.user.address.application.AddressService;
import shparos.user.global.common.response.BaseResponse;
import shparos.user.users.application.UserService;
import shparos.user.users.domain.User;
import shparos.user.address.dto.AddressModifyDto;
import shparos.user.address.dto.AddressRegisterDto;
import shparos.user.address.vo.AddressDefaultResponse;
import shparos.user.address.vo.AddressModifyRequest;
import shparos.user.address.vo.AddressResponse;
import shparos.user.address.vo.AddressRegisterRequest;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    /*
        주소리스트 조회
     */
    @Operation(summary = "주소리스트 조회", description = "등록되어 있는 주소리스트 전체 조회", tags = { "Address" })
    @GetMapping("/address")
    public BaseResponse<?> getAddressList(@RequestHeader("email") String email) {

        // 이메일로 유저정보 찾기
        User user = userService.getUserFromEmail(email);

        // 주소 리스트 조회
        List<AddressResponse> addressResponseList = addressService.getAddressList(user);
        return new BaseResponse<>(addressResponseList);
    }

    /*
        주소등록
     */
    @Operation(summary = "주소등록", description = "새로운 주소를 등록", tags = { "Address" })
    @PostMapping("/address")
    public BaseResponse<?> registerAddress(@RequestHeader("email") String email,
                                                  @RequestBody AddressRegisterRequest addressRegisterRequest) {

        // 이메일로 유저정보 찾기
        User user = userService.getUserFromEmail(email);

        AddressRegisterDto addressRegisterDto = AddressRegisterDto.builder()
                .user(user)
                .localAddress(addressRegisterRequest.getLocalAddress())
                .extraAddress(addressRegisterRequest.getExtraAddress())
                .defaultAddress(addressRegisterRequest.getDefaultAddress())
                .localCode(addressRegisterRequest.getLocalCode())
                .build();
        // 주소 등록
        addressService.registerAddress(addressRegisterDto);

        return new BaseResponse<>();
    }

    /*
        주소수정
     */
    @Operation(summary = "주소수정", description = "등록되어있는 주소를 수정", tags = { "Address" })
    @PutMapping("/address")
    public BaseResponse<?> modify(@RequestHeader("email") String email,
                                         @RequestBody AddressModifyRequest addressModifyRequest) {

        // 이메일로 유저정보 찾기
        User user = userService.getUserFromEmail(email);

        // 주소 수정
        AddressModifyDto addressModifyDto = AddressModifyDto.builder()
                .addressId(addressModifyRequest.getAddressId())
                .user(user)
                .localAddress(addressModifyRequest.getLocalAddress())
                .extraAddress(addressModifyRequest.getExtraAddress())
                .localCode(addressModifyRequest.getLocalCode())
                .build();
        addressService.modifyAddress(addressModifyDto);
        return new BaseResponse<>();
    }

    /*
        주소삭제
     */
    @Operation(summary = "주소삭제",
            description = "해당하는 주소를 삭제, 단 대표주소의 경우 삭제 불가",
            tags = { "Address" })
    @DeleteMapping("/address/{addressId}")
    public BaseResponse<?> deleteAddress(@RequestHeader("email") String email,
                                                @PathVariable("addressId") Long addressId) {
        // 주소 삭제
        addressService.deleteAddress(addressId);
        return new BaseResponse<>();
    }

    /*
        대표주소 조회
     */
    @Operation(summary = "대표주소 조회", description = "대표주소로 설정된 주소를 조회", tags = { "Address" })
    @GetMapping("/address/default")
    public BaseResponse<?> getDefaultAddress(@RequestHeader("email") String email) {

        // 이메일로 유저정보 찾기
        User user = userService.getUserFromEmail(email);
        AddressDefaultResponse addressDefaultResponse = addressService.getDefaultAddress(user);
        return new BaseResponse<>(addressDefaultResponse);
    }
}