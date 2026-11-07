package team.projectpulse.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.projectpulse.course.Course;
import team.projectpulse.course.CourseSecurityService;
import team.projectpulse.section.Section;
import team.projectpulse.section.SectionRepository;
import team.projectpulse.system.UserUtils;
import team.projectpulse.system.exception.ObjectNotFoundException;
import team.projectpulse.user.userinvitation.UserInvitationService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDeactivationServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    SectionRepository sectionRepository;
    @Mock
    UserInvitationService userInvitationService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserUtils userUtils;
    @Mock
    CourseSecurityService courseSecurityService;
    @Mock
    StudentSecurityService studentSecurityService;

    @InjectMocks
    StudentService studentService;

    Student student;

    private Student studentWithCourse() {
        Course course = mock(Course.class);
        given(course.getCourseId()).willReturn(1);
        Section section = mock(Section.class);
        given(section.getCourse()).willReturn(course);
        Student student = new Student("student", "Student", "One", "student@example.edu", "123456", true, "student");
        student.setSection(section);
        return student;
    }

    @Test
    void courseAdminCanDeactivateStudent() {
        this.student = this.studentWithCourse();
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.student));
        given(this.courseSecurityService.isCourseOwner(1)).willReturn(true);
        given(this.studentRepository.save(this.student)).willReturn(this.student);

        Student result = this.studentService.deactivateStudent(4);

        assertThat(result.isEnabled()).isFalse();
        verify(this.studentSecurityService, never()).isCurrentUserInstructorOfStudentSection(anyInt());
        verify(this.studentRepository).save(this.student);
    }

    @Test
    void assignedInstructorCanDeactivateStudent() {
        this.student = this.studentWithCourse();
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.student));
        given(this.courseSecurityService.isCourseOwner(1)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(true);
        given(this.studentRepository.save(this.student)).willReturn(this.student);

        Student result = this.studentService.deactivateStudent(4);

        assertThat(result.isEnabled()).isFalse();
        verify(this.studentSecurityService).isCurrentUserInstructorOfStudentSection(4);
        verify(this.studentRepository).save(this.student);
    }

    @Test
    void missingStudentReturnsNotFoundBeforeAuthorization() {
        given(this.studentRepository.findById(404)).willReturn(Optional.empty());

        assertThatThrownBy(() -> this.studentService.deactivateStudent(404))
                .isInstanceOf(ObjectNotFoundException.class);

        verifyNoInteractions(this.courseSecurityService, this.studentSecurityService);
    }

    @Test
    void unauthorizedCallerIsRefusedBeforeInactiveStatusIsChecked() {
        this.student = this.studentWithCourse();
        this.student.setEnabled(false);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.student));
        given(this.courseSecurityService.isCourseOwner(1)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(false);

        assertThatThrownBy(() -> this.studentService.deactivateStudent(4))
                .isInstanceOf(AccessDeniedException.class);

        verify(this.studentRepository, never()).save(any(Student.class));
    }

    @Test
    void authorizedCallerCannotDeactivateAnAlreadyInactiveStudent() {
        this.student = this.studentWithCourse();
        this.student.setEnabled(false);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.student));
        given(this.courseSecurityService.isCourseOwner(1)).willReturn(true);

        assertThatThrownBy(() -> this.studentService.deactivateStudent(4))
                .isInstanceOf(StudentAlreadyInactiveException.class);

        verify(this.studentRepository, never()).save(any(Student.class));
        verifyNoInteractions(this.studentSecurityService);
    }
}
