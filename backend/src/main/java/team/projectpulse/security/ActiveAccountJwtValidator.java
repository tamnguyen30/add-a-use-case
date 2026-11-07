package team.projectpulse.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger logger = LoggerFactory.getLogger(ActiveAccountJwtValidator.class);
    private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN,
            "The account is inactive or its status could not be verified.",
            null);

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = getUserId(jwt);
        if (userId == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        try {
            return this.userRepository.findById(userId)
                    .filter(PeerEvaluationUser::isEnabled)
                    .map(user -> OAuth2TokenValidatorResult.success())
                    .orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT));
        } catch (DataAccessException exception) {
            logger.warn("Unable to verify bearer-token account status; rejecting the token.", exception);
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }
    }

    private Integer getUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (!(claim instanceof Number number)) {
            return null;
        }

        long userId = number.longValue();
        if (userId < Integer.MIN_VALUE || userId > Integer.MAX_VALUE || number.doubleValue() != userId) {
            return null;
        }
        return (int) userId;
    }
}
