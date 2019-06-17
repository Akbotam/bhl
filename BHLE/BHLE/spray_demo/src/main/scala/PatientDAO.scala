class PatientDAO {
    var patients: Seq[Patient] = Seq.empty[Patient]
    def init_ (): Unit = {
        patients = patients :+ Patient("aida")
    }
    def getPatients = patients
    def getPatient = Patient("ulzhan")
}
