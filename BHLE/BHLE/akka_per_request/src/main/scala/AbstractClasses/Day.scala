package AbstractClasses

case class Day (id: Int){
    var daySchedule: Vector[Session] = Vector.empty[Session]
}
