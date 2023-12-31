package spharos.user.users.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spharos.user.address.application.AddressService;
import spharos.user.address.dto.AddressRegisterDto;
import spharos.user.address.dto.AddressRegisterResultDto;
import spharos.user.address.vo.AddressDefaultResponse;
import spharos.user.global.common.response.ResponseCode;
import spharos.user.users.domain.User;
import spharos.user.global.config.security.JwtTokenProvider;
import spharos.user.global.exception.CustomException;
import spharos.user.users.dto.UserModifyDto;
import spharos.user.users.dto.UserPasswordChangeDto;
import spharos.user.users.dto.UserPasswordCheckDto;
import spharos.user.users.dto.UserWithdrawCheckDto;
import spharos.user.users.infrastructure.UserRepository;
import spharos.user.users.vo.request.UserLoginRequest;
import spharos.user.users.vo.request.UserSignUpRequest;
import spharos.user.users.vo.response.UserFindEmailResponse;
import spharos.user.users.vo.response.UserInformationResponse;
import spharos.user.users.vo.response.UserLoginResponse;
import spharos.user.users.vo.response.UserSignUpResponse;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final AddressService addressService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 이메일 중복 체크
    @Override
    public Boolean checkEmailExist(String email) {

        Optional<User> user = userRepository.findByEmail(email);

        // 해당 이메일이 DB에 존재하면 체크 결과를 true로 리턴
        if(user.isPresent()){
            return Boolean.TRUE;
        }

        // 해당 이메일이 DB에 존재하지 않으면 체크 결과를 false로 리턴
        return Boolean.FALSE;
    }

    // 닉네임 중복 체크
    @Override
    public Boolean checkNickname(String nickname) {

        Optional<User> user = userRepository.findByNickname(nickname);

        // 해당 닉네임이 DB에 존재하면 체크 결과를 true로 리턴
        if(user.isPresent()){
            return Boolean.TRUE;
        }

        // 해당 닉네임이 DB에 존재하지 않으면 체크 결과를 false로 리턴
        return Boolean.FALSE;
    }

    // 회원가입
    @Override
    @Transactional
    public UserSignUpResponse join(UserSignUpRequest request) {

        // 비밀번호 암호화
        String hashedPassword = new BCryptPasswordEncoder().encode(request.getPassword());

        // 유저 등록
        User user = User.createUser(request.getEmail(), hashedPassword, request.getBirthday(), request.getUsername(),
                request.getNickname(), request.getPhone(), 0);
        userRepository.save(user);

        // 주소 등록
        AddressRegisterDto addressRegisterDto = AddressRegisterDto.builder()
                .user(user)
                .localAddress(request.getLocalAddress())
                .extraAddress(request.getExtraAddress())
                .defaultAddress(Boolean.TRUE)
                .localCode(request.getLocalCode())
                .build();
        AddressRegisterResultDto resultDto = addressService.registerAddress(addressRegisterDto);

        // 리턴값 지정하기
        return UserSignUpResponse.builder()
                .email(user.getEmail())
                .username(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .localAddress(resultDto.getLocalAddress())
                .extraAddress(resultDto.getExtraAddress())
                .build();
    }

    // 로그인
    @Override
    public UserLoginResponse login(UserLoginRequest userLoginIn) {

        // 유저 확인
        User user = userRepository.findByEmail(userLoginIn.getEmail())
                .orElseThrow(() -> new CustomException(ResponseCode.LOGIN_FAIL));

        // 유저 상태 확인
        if(user.getStatus() == 1) {
            // 탈퇴 유저인 경우
            throw new CustomException(ResponseCode.WITHDRAW_USER);
        } else if(user.getStatus() == 2) {
            // 휴면 유저인 경우
            throw new CustomException(ResponseCode.DORMANT_USER);
        }

        // 아이디와 비밀번호 확인
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLoginIn.getEmail(),
                        userLoginIn.getPassword()
                )
        );

        // 토큰발급
        String accessToken = jwtTokenProvider.generateToken(user);
        // 리프레시 토큰 발급 TODO
//        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 대표주소 조회
        AddressDefaultResponse defaultAddress = addressService.getDefaultAddress(user);

        return UserLoginResponse.builder()
                .token(accessToken)
                .email(user.getEmail())
                .username(user.getName())
                .address(defaultAddress.getLocalAddress() + " " + defaultAddress.getExtraAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    // 아이디(이메일) 찾기
    @Override
    public UserFindEmailResponse findEmail(String username, String phone) {

        // 휴대폰 번호로 일치하는 유저 정보 조회
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new CustomException(ResponseCode.NOT_EXISTS_USER_EMAIL));

        // 이름이 다른 경우
        if(!user.getName().equals(username)) {
            throw new CustomException(ResponseCode.NOT_EXISTS_USER_EMAIL);
        }

        // 유저 상태가 탈퇴인 경우
        if(user.getStatus() == 1) {
            throw new CustomException(ResponseCode.NOT_EXISTS_USER_EMAIL);
        }

        return UserFindEmailResponse.builder()
                .email(user.getEmail())
                .build();
    }

    // 비밀번호 변경
    @Override
    @Transactional
    public void modifyPassword(UserPasswordChangeDto userPasswordChangeDto) {

        // 비밀번호를 변경할 유저 확인
        User user = userRepository.findByEmail(userPasswordChangeDto.getEmail())
                .orElseThrow(() -> new CustomException(ResponseCode.CANNOT_FIND_USER));

        // 비밀번호 변경
        user.setPassword(new BCryptPasswordEncoder().encode(userPasswordChangeDto.getPassword()));
    }

    // 이메일로 유저정보 찾기
    @Override
    public User getUserFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ResponseCode.CANNOT_FIND_USER));
    }

    /*
        이름과 이메일로 해당하는 유저가 존재하는지 체크
     */
    @Override
    public Boolean checkExistEmailByNameAndEmail(String username, String email) {

        // 이메일로 유저 조회
        Optional<User> user = userRepository.findByEmail(email);

        // 해당 닉네임이 DB에 존재하지 않거나 유저 상태가 [정상] 이외면 체크 결과를 false로 리턴
        if(user.isEmpty() || user.get().getStatus() != 0) {
            return Boolean.FALSE;
        }

        // 이름이 일치 하지 않으면 체크 결과를 false로 리턴
        if(!user.get().getName().equals(username)) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    // 이메일과 비밀번호로 유저 확인
    @Override
    public Boolean checkPassword(UserPasswordCheckDto userPasswordCheckDto) {

        // 유저 확인
        User user = getUserFromEmail(userPasswordCheckDto.getEmail());

        // 비밀번호 일치 확인
        if(new BCryptPasswordEncoder().matches(userPasswordCheckDto.getPassword(),user.getPassword())) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    // 회원정보조회
    @Override
    public UserInformationResponse getUserInformation(String email) {

        // 유저 확인
        User user = getUserFromEmail(email);
        return UserInformationResponse.builder()
                .email(user.getEmail())
                .username(user.getName())
                .birthday(user.getBirthday())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .build();
    }

    // 회원정보 수정
    @Override
    @Transactional
    public void modifyUserInformation(UserModifyDto userModifyDto) {

        // 유저 확인
        User user = getUserFromEmail(userModifyDto.getEmail());

        // 정보를 수정함
        user.modifyUser(userModifyDto.getBirthday(),
                userModifyDto.getUsername(),
                userModifyDto.getNickname(),
                userModifyDto.getPhone());
    }

    // 회원탈퇴전 회원확인
    @Override
    public Boolean checkUserBeforeWithdraw(UserWithdrawCheckDto dto) {

        // 헤더에 담긴 이메일과 유저가 입력한 이메일이 다른 경우
        if(!dto.getLoginEmail().equals(dto.getInputEmail())) {
            throw new CustomException(ResponseCode.CANNOT_FIND_USER);
        }

        // 유저 확인
        User user = getUserFromEmail(dto.getLoginEmail());

        // 비밀번호 일치 확인
        if(!new BCryptPasswordEncoder().matches(dto.getPassword(),user.getPassword())) {
            return Boolean.FALSE;
        }

        // 이름 일치 확인
        if(!user.getName().equals(dto.getUsername())) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    // 회원탈퇴
    @Override
    @Transactional
    public void withdrawUser(String email) {

        // 유저 확인
        User user = getUserFromEmail(email);

        // 유저 상태를 [탈퇴]로 변경
        user.setStatus(1);
    }


}
