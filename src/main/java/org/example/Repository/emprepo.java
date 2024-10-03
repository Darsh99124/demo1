package org.example.Repository;

import org.example.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface emprepo extends JpaRepository<Employee,Integer> {

}
