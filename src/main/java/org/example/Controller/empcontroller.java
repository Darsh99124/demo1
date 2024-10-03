package org.example.Controller;

import org.example.Employee;
import org.example.Repository.emprepo;
import org.example.Service.empservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/methods")
public class empcontroller {
@Autowired
    public empservice emps;
@Autowired
public emprepo emprepo1;


@PostMapping("/pp")
public ResponseEntity<List<Employee>> view(@RequestBody List<Employee> emp)
{

    return ResponseEntity.ok(emps.add(emp));

}
@PutMapping("/pm/{id}")
    public ResponseEntity<Employee> update(@PathVariable int id,@RequestBody Employee updatedEmp)
{

    Employee existingEmployee = emprepo1.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

    existingEmployee.setEmpname(updatedEmp.getEmpname());
    existingEmployee.setEmpsal(updatedEmp.getEmpsal());


    emprepo1.save(existingEmployee);

    return ResponseEntity.ok(existingEmployee);

}

@GetMapping("/gm/{id}")
    public ResponseEntity<Employee> show(@PathVariable int id)
{

    Employee employee = emprepo1.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    return ResponseEntity.ok(employee);
}
@GetMapping("/gm")
public List<Employee> get()
{
    return emprepo1.findAll();
}
@DeleteMapping("/dm/{id}")
    public String del(@PathVariable int id)
{
    Employee employee = emprepo1.findById(id)
        .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    emprepo1.delete(employee);
    return "Employee with ID " + id + " has been deleted successfully.";
}

}
