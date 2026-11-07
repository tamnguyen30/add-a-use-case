package team.projectpulse.student;

public class StudentAlreadyInactiveException extends RuntimeException {

    public StudentAlreadyInactiveException(Integer studentId) {
        super("Student " + studentId + " is already inactive.");
    }
}
