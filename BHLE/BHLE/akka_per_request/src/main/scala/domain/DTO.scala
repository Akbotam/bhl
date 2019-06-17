
package domain

import model.{FullPatient, Patient}

trait DomainObject extends Serializable

trait DomainEntity extends DomainObject

case class GetPatients() extends DomainEntity
case class AddPatient(book: FullPatient) extends DomainEntity
case class UpdatePatient(p: Patient) extends DomainEntity
case class DeletePatient(id: Long) extends DomainEntity