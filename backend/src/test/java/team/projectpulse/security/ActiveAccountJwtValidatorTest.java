package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;

    private ActiveAccountJwtValidator validator;


    @BeforeEach
    void setUp() {
        this.validator = new ActiveAccountJwtValidator(this.userRepository);
    }

    @Test
    void acceptsJwtForAnActiveAccount() {
        Student student = new Student();
        student.setEnabled(true);
        given(this.userRepository.findById(4)).willReturn(Optional.of(student));

        assertThat(this.validator.validate(jwtWithUserId(4)).hasErrors()).isFalse();
    }

    @Test
    void rejectsJwtForAnInactiveAccount() {
        Student student = new Student();
        student.setEnabled(false);
        given(this.userRepository.findById(4)).willReturn(Optional.of(student));

        assertThat(this.validator.validate(jwtWithUserId(4)).hasErrors()).isTrue();
    }

    @Test
    void rejectsJwtWhenTheAccountDoesNotExist() {
        given(this.userRepository.findById(4)).willReturn(Optional.empty());

        assertThat(this.validator.validate(jwtWithUserId(4)).hasErrors()).isTrue();
    }

    @Test
    void rejectsJwtWithoutANumericUserId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("unrelated", "value")
                .build();

        assertThat(this.validator.validate(jwt).hasErrors()).isTrue();
        verifyNoInteractions(this.userRepository);
    }

    @Test
    void rejectsJwtWhenAccountStatusCannotBeRead() {
        given(this.userRepository.findById(4)).willThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThat(this.validator.validate(jwtWithUserId(4)).hasErrors()).isTrue();
    }

    private static Jwt jwtWithUserId(int userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("userId", userId)
                .build();
    }
}
