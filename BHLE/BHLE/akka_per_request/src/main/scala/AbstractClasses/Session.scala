package AbstractClasses

object Session {
    def apply(s: String): Session = {
        val st = s.substring(1, s.length - 1).split(',')
        new Session(st(0).toInt, st(1).toInt)
    }
}

case class Session(time: Int, id: Int) {

}
